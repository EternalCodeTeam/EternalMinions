package com.eternalcode.minions.minion;

import com.eternalcode.minions.config.MinionsConfig;
import com.eternalcode.minions.database.MinionPersistenceService;
import com.eternalcode.minions.minion.activity.ActivityDecision;
import com.eternalcode.minions.minion.activity.MinionActivityService;
import com.eternalcode.minions.minion.activity.MinionExecutionPolicy;
import com.eternalcode.minions.minion.status.MinionStatus;
import com.eternalcode.minions.minion.status.MinionStatusTracker;
import com.eternalcode.minions.render.MinionRenderer;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.kyori.adventure.key.Key;
import org.bukkit.Server;
import org.bukkit.World;

public final class MinionActionEngine implements Runnable {

    private static final long ACTIVE_INTERVAL_TICKS = 40L;
    private static final long IDLE_INTERVAL_TICKS = 100L;

    private final Server server;
    private final MinionRegistry minions;
    private final MinionsConfig config;
    private final MinionBehaviorRegistry behaviors;
    private final MinionPersistenceService persistence;
    private final MinionStatusTracker statuses;
    private final MinionRenderer renderer;
    private final MinionActivityService activity;
    private final MinionSchedule schedule = new MinionSchedule(128);
    private final Long2ObjectOpenHashMap<ScheduledMinion> scheduled = new Long2ObjectOpenHashMap<>();
    private long currentTick;

    public MinionActionEngine(
            Server server,
            MinionRegistry minions,
            MinionsConfig config,
            MinionBehaviorRegistry behaviors,
            MinionPersistenceService persistence,
            MinionStatusTracker statuses,
            MinionRenderer renderer,
            MinionActivityService activity
    ) {
        this.server = server;
        this.minions = minions;
        this.config = config;
        this.behaviors = behaviors;
        this.persistence = persistence;
        this.statuses = statuses;
        this.renderer = renderer;
        this.activity = activity;
    }

    public void add(Minion minion) {
        ScheduledMinion scheduledMinion = new ScheduledMinion(minion.id());
        this.scheduled.put(minion.id().value(), scheduledMinion);
        this.schedule.schedule(scheduledMinion, this.currentTick + this.workInterval(minion));
    }

    public void remove(Minion minion) {
        this.scheduled.remove(minion.id().value());
        this.schedule.cancel(minion.id());
    }

    @Override
    public void run() {
        this.currentTick++;
        long deadlineNanos = System.nanoTime() + this.config.schedulerBudgetMicros * 1_000L;
        int actions = 0;

        while (actions < this.config.physicalActionsPerTick && System.nanoTime() < deadlineNanos) {
            ScheduledMinion scheduledMinion = this.schedule.pollDue(this.currentTick);
            if (scheduledMinion == null) {
                return;
            }

            Minion minion = this.minions.findMinion(scheduledMinion.id()).orElse(null);
            if (minion == null) {
                this.scheduled.remove(scheduledMinion.id().value());
                continue;
            }

            long delayTicks = this.execute(minion, scheduledMinion);
            this.schedule.schedule(scheduledMinion, this.currentTick + delayTicks);
            actions++;
        }
    }

    private long execute(Minion minion, ScheduledMinion scheduledMinion) {
        if (!minion.active()) {
            return this.idleInterval(minion);
        }

        MinionBehavior behavior = this.behaviors.find(minion.behaviorId()).orElse(null);
        if (behavior == null) {
            return IDLE_INTERVAL_TICKS;
        }

        World world = this.server.getWorld(Key.key(minion.position().worldKey()));
        if (world == null) {
            return behavior.idleInterval();
        }

        ActivityDecision decision = this.activity.evaluate(minion, world);
        if (decision.frozen()) {
            this.applyStatusOnly(minion, decision.statusOverride());
            return behavior.idleInterval();
        }

        MinionExecutionPolicy policy = new MinionExecutionPolicy(!decision.suppressStorageGain());
        MinionResult result = behavior.execute(new MinionContext(minion, world, scheduledMinion, policy));
        if (decision.statusOverride() != null && result.status() != decision.statusOverride()) {
            result = new MinionResult(result.minion(), decision.statusOverride(), result.worked(), result.delayTicks());
        }

        this.apply(minion, result, scheduledMinion);

        long baseDelay = result.delayTicks() != null
                ? result.delayTicks()
                : result.worked() ? behavior.workInterval(result.minion()) : behavior.idleInterval();

        if (decision.speedMultiplier() < 1.0D) {
            return Math.max(1L, Math.round(baseDelay / decision.speedMultiplier()));
        }
        return baseDelay;
    }

    private void applyStatusOnly(Minion minion, MinionStatus status) {
        boolean statusChanged = this.statuses.setStatus(minion.id(), status);
        if (statusChanged) {
            this.renderer.refreshHologram(minion);
        }
    }

    private void apply(Minion previous, MinionResult result, ScheduledMinion scheduledMinion) {
        Minion updated = result.minion();
        boolean equipmentChanged = previous.equipment() != updated.equipment();
        boolean storageChanged = previous.storage() != updated.storage();
        boolean progressChanged = previous.progress() != updated.progress();

        if (previous != updated) {
            this.minions.replace(updated);
        }
        if (equipmentChanged) {
            this.persistence.saveEquipment(updated);
            this.renderer.refreshEquipment(updated.id(), updated.equipment().tool());
        }
        if (storageChanged) {
            this.persistence.saveStorage(previous, updated);
        }
        if (progressChanged) {
            this.persistence.saveState(updated);
        }

        boolean statusChanged = this.statuses.setStatus(updated.id(), result.status());
        boolean levelChanged = previous.progress().level() != updated.progress().level();
        if (statusChanged || levelChanged) {
            this.renderer.refreshHologram(updated);
        }

        float animationYaw = scheduledMinion.consumeAnimationYaw();
        if (result.worked() && !Float.isNaN(animationYaw)) {
            this.renderer.animate(updated.id(), animationYaw);
        }
    }

    private long workInterval(Minion minion) {
        MinionBehavior behavior = this.behaviors.find(minion.behaviorId()).orElse(null);
        return behavior == null ? ACTIVE_INTERVAL_TICKS : behavior.workInterval(minion);
    }

    private long idleInterval(Minion minion) {
        MinionBehavior behavior = this.behaviors.find(minion.behaviorId()).orElse(null);
        return behavior == null ? IDLE_INTERVAL_TICKS : behavior.idleInterval();
    }
}
