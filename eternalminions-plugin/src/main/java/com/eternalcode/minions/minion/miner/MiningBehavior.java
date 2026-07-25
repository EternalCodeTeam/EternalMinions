package com.eternalcode.minions.minion.miner;

import com.eternalcode.minions.database.MinionPersistenceService;
import com.eternalcode.minions.minion.AbstractMinionBehavior;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionDirection;
import com.eternalcode.minions.minion.MinionPosition;
import com.eternalcode.minions.minion.MinionRegistry;
import com.eternalcode.minions.minion.MinionType;
import com.eternalcode.minions.minion.ScheduledMinion;
import com.eternalcode.minions.minion.status.MinionStatusTracker;
import com.eternalcode.minions.minion.tool.SpeedEnchant;
import com.eternalcode.minions.minion.tool.ToolCheck;
import com.eternalcode.minions.minion.tool.ToolDurabilityService;
import com.eternalcode.minions.minion.tool.ToolInventoryLocator;
import com.eternalcode.minions.minion.tool.ToolValidationService;
import com.eternalcode.minions.render.MinionRenderer;
import java.util.Collection;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

// Mines one block per scheduled action (no per-block break-time simulation - kept simple on
// purpose). Fortune/Silk Touch/Unbreaking are always respected via the real Bukkit drop/durability
// APIs; Efficiency additionally speeds up the minion's action cadence, but only when the type's
// respectSpeedEnchants() config flag is enabled.
public final class MiningBehavior extends AbstractMinionBehavior {

    private final ToolValidationService toolValidation;
    private final ToolDurabilityService toolDurability;
    private final ToolInventoryLocator toolLocator;

    public MiningBehavior(
        MinionRegistry registry,
        MinionPersistenceService persistence,
        MinionRenderer renderer,
        MinionStatusTracker statuses,
        ToolValidationService toolValidation,
        ToolDurabilityService toolDurability,
        ToolInventoryLocator toolLocator
    ) {
        super(registry, persistence, renderer, statuses);
        this.toolValidation = toolValidation;
        this.toolDurability = toolDurability;
        this.toolLocator = toolLocator;
    }

    @Override
    public boolean execute(Minion minion, MinionType type, ScheduledMinion scheduledMinion, World world) {
        minion = this.retireWornToolIfNeeded(minion, type, world, this.toolValidation, this.toolLocator);

        ToolCheck toolCheck = this.toolValidation.validate(
            type.work().toolRequirement(), minion.equipment().tool(), MinerStatuses.NO_PICKAXE);
        if (toolCheck instanceof ToolCheck.Stopped stopped) {
            this.refreshStatusIfChanged(minion, stopped.reason());
            return false;
        }

        MineResult result = minion.settings().miningMode() == MiningMode.LINEAR
            ? this.executeLinear(minion, type, scheduledMinion, world)
            : this.executeSquare(minion, type, scheduledMinion, world);

        if (result == MineResult.NO_TARGET) {
            this.refreshStatusIfChanged(minion, MinerStatuses.NO_BLOCKS_IN_RANGE);
        }
        if (result == MineResult.MINED && type.respectSpeedEnchants()) {
            long baseInterval = type.workIntervalTicks(minion.upgrades());
            scheduledMinion.forceNextDelay(SpeedEnchant.scaledInterval(baseInterval, minion.equipment().tool()));
        }
        return result == MineResult.MINED;
    }

    private MineResult executeSquare(Minion minion, MinionType type, ScheduledMinion scheduledMinion, World world) {
        int radius = type.miningRadius(minion.upgrades());
        for (int checkedTargets = 0; checkedTargets < MinionMiningTargets.count(radius); checkedTargets++) {
            int targetIndex = scheduledMinion.miningTargetIndex(MinionMiningTargets.count(radius));
            scheduledMinion.advanceMiningTarget(MinionMiningTargets.count(radius));
            int targetX = minion.position().blockX() + MinionMiningTargets.offsetX(radius, targetIndex);
            int targetY = minion.position().blockY() + MinionMiningTargets.offsetY();
            int targetZ = minion.position().blockZ() + MinionMiningTargets.offsetZ(radius, targetIndex);
            MineResult result = this.tryMine(
                minion, type, world, targetX, targetY, targetZ, MinionMiningTargets.yaw(radius, targetIndex));
            if (result != MineResult.NO_TARGET) {
                return result;
            }
        }
        return MineResult.NO_TARGET;
    }

    private MineResult executeLinear(Minion minion, MinionType type, ScheduledMinion scheduledMinion, World world) {
        MinionDirection direction = minion.settings().direction();
        int length = 2 * type.miningRadius(minion.upgrades()) + 1;
        for (int checkedTargets = 0; checkedTargets < length; checkedTargets++) {
            int distance = scheduledMinion.miningTargetIndex(length) + 1;
            scheduledMinion.advanceMiningTarget(length);
            int targetX = minion.position().blockX() + direction.offsetX() * distance;
            int targetY = minion.position().blockY() + MinionMiningTargets.offsetY();
            int targetZ = minion.position().blockZ() + direction.offsetZ() * distance;
            MineResult result = this.tryMine(minion, type, world, targetX, targetY, targetZ, direction.yaw());
            if (result != MineResult.NO_TARGET) {
                return result;
            }
        }
        return MineResult.NO_TARGET;
    }

    private MineResult tryMine(Minion minion, MinionType type, World world, int x, int y, int z, float targetYaw) {
        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            return MineResult.NO_TARGET;
        }

        Block block = world.getBlockAt(x, y, z);
        if (!this.canMine(minion, type, block)) {
            return MineResult.NO_TARGET;
        }

        ItemStack tool = minion.equipment().tool();
        ToolCheck toolCheck = this.toolValidation.validateAgainstBlock(
            type.work().toolRequirement(), tool, block, MinerStatuses.TOOL_TOO_WEAK);
        if (toolCheck instanceof ToolCheck.Stopped stopped) {
            this.refreshStatusIfChanged(minion, stopped.reason());
            return MineResult.STOPPED;
        }

        Collection<ItemStack> drops = tool == null ? block.getDrops() : block.getDrops(tool);
        // Real physics update (not a silent world edit) so adjacent water/lava still react -
        // e.g. a cobblestone/obsidian generator keeps producing after the minion mines it.
        block.setType(Material.AIR, true);
        Minion updated = this.consumeTool(minion, tool);
        this.deposit(updated, type, world, block.getLocation(), drops, targetYaw);
        this.refreshStatusIfChanged(updated, MinerStatuses.MINING);
        return MineResult.MINED;
    }

    private Minion consumeTool(Minion minion, ItemStack tool) {
        if (tool == null) {
            return minion;
        }

        ItemStack damagedTool = this.toolDurability.consume(tool, 1);
        Minion updated = minion.withEquipment(minion.equipment().withTool(damagedTool));
        this.registry.replace(updated);
        this.persistence.saveEquipment(updated);
        this.renderer.refreshEquipment(updated.id(), damagedTool);
        return updated;
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

    private enum MineResult {
        MINED,
        NO_TARGET,
        STOPPED
    }
}
