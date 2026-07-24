package com.eternalcode.minions.minion;

import com.eternalcode.minions.config.MinionsConfig;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Map;
import net.kyori.adventure.key.Key;
import org.bukkit.Server;
import org.bukkit.World;

public final class MinionActionEngine implements Runnable {

    private static final long ACTIVE_INTERVAL_TICKS = 40L;
    private static final long IDLE_INTERVAL_TICKS = 100L;

    private final Server server;
    private final MinionRegistry registry;
    private final MinionsConfig config;
    private final MinionTypeService types;
    private final Map<MinionBehaviorType, MinionBehavior> behaviors;
    private final MinionSchedule schedule = new MinionSchedule(128);
    private final Long2ObjectOpenHashMap<ScheduledMinion> scheduled = new Long2ObjectOpenHashMap<>();
    private long currentTick;

    public MinionActionEngine(
        Server server,
        MinionRegistry registry,
        MinionsConfig config,
        MinionTypeService types,
        Map<MinionBehaviorType, MinionBehavior> behaviors
    ) {
        this.server = server;
        this.registry = registry;
        this.config = config;
        this.types = types;
        this.behaviors = Map.copyOf(behaviors);
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

            Minion minion = this.registry.findMinion(scheduledMinion.id()).orElse(null);
            if (minion == null) {
                this.scheduled.remove(scheduledMinion.id().value());
                continue;
            }

            long interval = this.execute(minion, scheduledMinion) ? this.workInterval(minion) : this.idleInterval(minion);
            this.schedule.schedule(scheduledMinion, this.currentTick + interval);
            actions++;
        }
    }

    private boolean execute(Minion minion, ScheduledMinion scheduledMinion) {
        if (!minion.active()) {
            return false;
        }

        MinionType type = this.types.type(minion.behaviorId()).orElse(null);
        if (type == null) {
            return false;
        }

        MinionBehavior behavior = this.behaviors.get(type.behavior());
        if (behavior == null) {
            return false;
        }

        World world = this.server.getWorld(Key.key(minion.position().worldKey()));
        if (world == null) {
            return false;
        }

        return behavior.execute(minion, type, scheduledMinion, world);
    }

    private long workInterval(Minion minion) {
        MinionType type = this.types.type(minion.behaviorId()).orElse(null);
        return type == null ? ACTIVE_INTERVAL_TICKS : type.workIntervalTicks(minion.upgrades());
    }

    private long idleInterval(Minion minion) {
        MinionType type = this.types.type(minion.behaviorId()).orElse(null);
        return type == null ? IDLE_INTERVAL_TICKS : type.idleIntervalTicks();
    }
}
