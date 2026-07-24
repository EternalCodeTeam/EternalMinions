package com.eternalcode.minions.minion.miner;

import com.eternalcode.minions.database.MinionPersistenceService;
import com.eternalcode.minions.minion.AbstractMinionBehavior;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionDirection;
import com.eternalcode.minions.minion.MinionPosition;
import com.eternalcode.minions.minion.MinionRegistry;
import com.eternalcode.minions.minion.MinionType;
import com.eternalcode.minions.minion.ScheduledMinion;
import com.eternalcode.minions.render.MinionRenderer;
import java.util.Collection;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

public final class MiningBehavior extends AbstractMinionBehavior {

    public MiningBehavior(MinionRegistry registry, MinionPersistenceService persistence, MinionRenderer renderer) {
        super(registry, persistence, renderer);
    }

    @Override
    public boolean execute(Minion minion, MinionType type, ScheduledMinion scheduledMinion, World world) {
        return minion.settings().miningMode() == MiningMode.LINEAR
            ? this.executeLinear(minion, type, scheduledMinion, world)
            : this.executeSquare(minion, type, scheduledMinion, world);
    }

    private boolean executeSquare(Minion minion, MinionType type, ScheduledMinion scheduledMinion, World world) {
        int radius = type.miningRadius(minion.upgrades());
        for (int checkedTargets = 0; checkedTargets < MinionMiningTargets.count(radius); checkedTargets++) {
            int targetIndex = scheduledMinion.miningTargetIndex(MinionMiningTargets.count(radius));
            scheduledMinion.advanceMiningTarget(MinionMiningTargets.count(radius));
            int targetX = minion.position().blockX() + MinionMiningTargets.offsetX(radius, targetIndex);
            int targetY = minion.position().blockY() + MinionMiningTargets.offsetY();
            int targetZ = minion.position().blockZ() + MinionMiningTargets.offsetZ(radius, targetIndex);
            if (this.tryMine(minion, type, world, targetX, targetY, targetZ, MinionMiningTargets.yaw(radius, targetIndex))) {
                return true;
            }
        }
        return false;
    }

    private boolean executeLinear(Minion minion, MinionType type, ScheduledMinion scheduledMinion, World world) {
        MinionDirection direction = minion.settings().direction();
        int length = 2 * type.miningRadius(minion.upgrades()) + 1;
        for (int checkedTargets = 0; checkedTargets < length; checkedTargets++) {
            int distance = scheduledMinion.miningTargetIndex(length) + 1;
            scheduledMinion.advanceMiningTarget(length);
            int targetX = minion.position().blockX() + direction.offsetX() * distance;
            int targetY = minion.position().blockY() + MinionMiningTargets.offsetY();
            int targetZ = minion.position().blockZ() + direction.offsetZ() * distance;
            if (this.tryMine(minion, type, world, targetX, targetY, targetZ, direction.yaw())) {
                return true;
            }
        }
        return false;
    }

    private boolean tryMine(Minion minion, MinionType type, World world, int x, int y, int z, float targetYaw) {
        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            return false;
        }

        Block block = world.getBlockAt(x, y, z);
        if (!this.canMine(minion, type, block)) {
            return false;
        }

        ItemStack tool = minion.equipment().tool();
        Collection<ItemStack> drops = tool == null ? block.getDrops() : block.getDrops(tool);
        block.setType(Material.AIR, false);
        this.deposit(minion, type, world, block.getLocation(), drops, targetYaw);
        return true;
    }

    private boolean canMine(Minion minion, MinionType type, Block block) {
        if (block.isEmpty() || block.isLiquid() || !type.canMine(block.getType())) {
            return false;
        }
        MinionPosition chest = minion.chestPosition();
        return chest == null
            || chest.blockX() != block.getX()
            || chest.blockY() != block.getY()
            || chest.blockZ() != block.getZ()
            || !chest.worldKey().equals(minion.position().worldKey());
    }
}
