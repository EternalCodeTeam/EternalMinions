package com.eternalcode.minions.minion.schedule;

import com.eternalcode.minions.config.MinionsConfig;
import com.eternalcode.minions.database.MinionPersistenceService;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionRegistry;
import com.eternalcode.minions.minion.activity.ActivityDecision;
import com.eternalcode.minions.minion.activity.MinionActivityService;
import com.eternalcode.minions.minion.behavior.MinionBehavior;
import com.eternalcode.minions.minion.behavior.MinionBehaviorRegistry;
import com.eternalcode.minions.minion.behavior.MinionContext;
import com.eternalcode.minions.minion.behavior.MinionResult;
import com.eternalcode.minions.minion.status.MinionStatus;
import com.eternalcode.minions.minion.status.MinionStatusTracker;
import com.eternalcode.minions.render.MinionRenderer;
import net.kyori.adventure.key.Key;
import org.bukkit.Server;
import org.bukkit.World;

public final class MinionScheduler implements Runnable {

    private static final long ACTIVE_INTERVAL_TICKS = 40L;
    private static final long IDLE_INTERVAL_TICKS = 100L;
    private static final int DEADLINE_CHECK_INTERVAL = 8;

    private final Server server;
    private final MinionRegistry minionRegistry;
    private final MinionsConfig schedulerConfig;
    private final MinionBehaviorRegistry behaviorRegistry;
    private final MinionPersistenceService persistenceService;
    private final MinionStatusTracker statusTracker;
    private final MinionRenderer renderer;
    private final MinionActivityService activityService;
    private final MinionSchedule minionSchedule = new MinionSchedule(128);
    private long currentTick;

    public MinionScheduler(
            Server server,
            MinionRegistry minionRegistry,
            MinionsConfig schedulerConfig,
            MinionBehaviorRegistry behaviorRegistry,
            MinionPersistenceService persistenceService,
            MinionStatusTracker statusTracker,
            MinionRenderer renderer,
            MinionActivityService activityService
    ) {
        this.server = server;
        this.minionRegistry = minionRegistry;
        this.schedulerConfig = schedulerConfig;
        this.behaviorRegistry = behaviorRegistry;
        this.persistenceService = persistenceService;
        this.statusTracker = statusTracker;
        this.renderer = renderer;
        this.activityService = activityService;
    }

    public void add(Minion minion) {
        ScheduledMinion scheduledMinion = new ScheduledMinion(minion.id());
        this.minionSchedule.schedule(scheduledMinion, this.currentTick + this.initialDelay(minion));
    }

    public void remove(Minion minion) {
        this.minionSchedule.cancel(minion.id());
    }

    @Override
    public void run() {
        this.currentTick++;
        long deadlineNanos = System.nanoTime() + this.schedulerConfig.schedulerBudgetMicros * 1_000L;
        int executedActions = 0;

        while (executedActions < this.schedulerConfig.physicalActionsPerTick) {
            if (executedActions % DEADLINE_CHECK_INTERVAL == 0 && System.nanoTime() >= deadlineNanos) {
                return;
            }

            ScheduledMinion scheduledMinion = this.minionSchedule.pollDue(this.currentTick);
            if (scheduledMinion == null) {
                return;
            }

            Minion minion = this.minionRegistry.findMinion(scheduledMinion.minionId()).orElse(null);
            if (minion == null) {
                continue;
            }

            long delayTicks = this.executeCycle(minion, scheduledMinion);
            this.minionSchedule.schedule(scheduledMinion, this.currentTick + delayTicks);
            executedActions++;
        }
    }

    private long executeCycle(Minion minion, ScheduledMinion scheduledMinion) {
        MinionBehavior behavior = this.behaviorRegistry.find(minion.behaviorId()).orElse(null);
        if (behavior == null) {
            return IDLE_INTERVAL_TICKS;
        }

        World world = this.server.getWorld(Key.key(minion.position().worldKey()));
        if (world == null) {
            return behavior.idleInterval();
        }

        ActivityDecision decision = this.activityService.evaluate(minion, world);
        if (decision.frozen()) {
            this.applyStatusOnly(minion, decision.statusOverride());
            return behavior.idleInterval();
        }

        MinionResult result = behavior.execute(new MinionContext(
                minion,
                world,
                scheduledMinion,
                decision.executionPolicy()
        ));
        if (decision.statusOverride() != null && result.status() != decision.statusOverride()) {
            result = new MinionResult(result.minion(), decision.statusOverride(), result.worked(), result.delayTicks());
        }

        this.applyResult(minion, result, scheduledMinion);

        long baseDelay = result.delayTicks() != null
                ? result.delayTicks()
                : result.worked() ? behavior.workInterval(result.minion()) : behavior.idleInterval();

        if (decision.speedMultiplier() < 1.0D) {
            return Math.max(1L, Math.round(baseDelay / decision.speedMultiplier()));
        }
        return baseDelay;
    }

    private void applyStatusOnly(Minion minion, MinionStatus status) {
        boolean statusChanged = this.statusTracker.setStatus(minion.id(), status);
        if (statusChanged) {
            this.renderer.refreshHologram(minion);
        }
    }

    private void applyResult(Minion previous, MinionResult result, ScheduledMinion scheduledMinion) {
        Minion updated = result.minion();
        boolean equipmentChanged = previous.equipment() != updated.equipment();
        boolean storageChanged = previous.storage() != updated.storage();
        boolean progressChanged = previous.progress() != updated.progress();

        if (previous != updated) {
            this.minionRegistry.replace(updated);
        }
        if (equipmentChanged) {
            boolean visualChange = updated.equipment().hasVisualChangeSince(previous.equipment());
            if (visualChange) {
                this.renderer.refreshEquipment(updated.id(), updated.equipment().tool());
            }
        }

        if (equipmentChanged || storageChanged || progressChanged) {
            this.persistenceService.saveAction(previous, updated);
        }

        boolean statusChanged = this.statusTracker.setStatus(updated.id(), result.status());
        boolean levelChanged = previous.progress().level() != updated.progress().level();
        if (statusChanged || levelChanged) {
            this.renderer.refreshHologram(updated);
        }

        float animationYaw = scheduledMinion.consumeAnimationYaw();
        if (result.worked() && !Float.isNaN(animationYaw)) {
            this.renderer.animate(updated.id(), animationYaw);
        }
    }

    private long initialDelay(Minion minion) {
        MinionBehavior behavior = this.behaviorRegistry.find(minion.behaviorId()).orElse(null);
        return behavior == null ? ACTIVE_INTERVAL_TICKS : behavior.workInterval(minion);
    }

}
