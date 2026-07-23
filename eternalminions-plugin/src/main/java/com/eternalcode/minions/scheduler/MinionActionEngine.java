package com.eternalcode.minions.scheduler;

import com.eternalcode.minions.config.MinionsConfig;
import com.eternalcode.minions.database.MinionPersistenceService;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionRegistry;
import com.eternalcode.minions.minion.MinionStorage;
import com.eternalcode.minions.minion.MinionStorageUpdate;
import com.eternalcode.minions.render.MinionRenderer;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Collection;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

public final class MinionActionEngine implements Runnable {

    private static final long ACTIVE_INTERVAL_TICKS = 40L;
    private static final long IDLE_INTERVAL_TICKS = 100L;

    private final Server server;
    private final MinionRegistry registry;
    private final MinionRenderer renderer;
    private final MinionPersistenceService persistence;
    private final MinionsConfig config;
    private final MinionSchedule schedule = new MinionSchedule(128);
    private final Long2ObjectOpenHashMap<ScheduledMinion> scheduled = new Long2ObjectOpenHashMap<>();
    private long currentTick;

    public MinionActionEngine(
        Server server,
        MinionRegistry registry,
        MinionRenderer renderer,
        MinionPersistenceService persistence,
        MinionsConfig config
    ) {
        this.server = server;
        this.registry = registry;
        this.renderer = renderer;
        this.persistence = persistence;
        this.config = config;
    }

    public void add(Minion minion) {
        ScheduledMinion scheduledMinion = new ScheduledMinion(minion.id());
        this.scheduled.put(minion.id().value(), scheduledMinion);
        this.schedule.schedule(scheduledMinion, this.currentTick + ACTIVE_INTERVAL_TICKS);
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

            long interval = this.execute(minion, scheduledMinion) ? ACTIVE_INTERVAL_TICKS : IDLE_INTERVAL_TICKS;
            this.schedule.schedule(scheduledMinion, this.currentTick + interval);
            actions++;
        }
    }

    private boolean execute(Minion minion, ScheduledMinion scheduledMinion) {
        if (!minion.active()) {
            return false;
        }

        World world = this.server.getWorld(Key.key(minion.position().worldKey()));
        if (world == null) {
            return false;
        }

        for (int checkedTargets = 0; checkedTargets < MinionMiningTargets.count(); checkedTargets++) {
            int targetIndex = scheduledMinion.miningTargetIndex();
            scheduledMinion.advanceMiningTarget();
            int targetX = minion.position().blockX() + MinionMiningTargets.offsetX(targetIndex);
            int targetY = minion.position().blockY() + MinionMiningTargets.offsetY();
            int targetZ = minion.position().blockZ() + MinionMiningTargets.offsetZ(targetIndex);
            if (!world.isChunkLoaded(targetX >> 4, targetZ >> 4)) {
                continue;
            }

            Block block = world.getBlockAt(targetX, targetY, targetZ);
            if (!this.canMine(block)) {
                continue;
            }

            this.mine(minion, world, block, MinionMiningTargets.yaw(targetIndex));
            return true;
        }
        return false;
    }

    private boolean canMine(Block block) {
        return !block.isEmpty() && !block.isLiquid() && block.getType() != Material.BEDROCK;
    }

    private void mine(Minion minion, World world, Block block, float targetYaw) {
        ItemStack tool = minion.equipment().tool();
        Collection<ItemStack> drops = tool == null ? block.getDrops() : block.getDrops(tool);
        block.setType(Material.AIR, false);
        MinionStorage storage = minion.storage();
        for (ItemStack drop : drops) {
            MinionStorageUpdate update = storage.add(drop);
            storage = update.storage();
            ItemStack remaining = update.remaining();
            if (remaining != null) {
                world.dropItemNaturally(block.getLocation(), remaining);
            }
        }

        Minion updatedMinion = minion.withStorage(storage);
        this.registry.replace(updatedMinion);
        this.persistence.changed(updatedMinion);
        this.renderer.animate(minion.id(), targetYaw);
    }
}
