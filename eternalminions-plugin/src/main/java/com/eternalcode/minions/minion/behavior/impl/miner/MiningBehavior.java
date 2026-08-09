package com.eternalcode.minions.minion.behavior.impl.miner;

import com.eternalcode.minions.config.AbstractMinionConfig;
import com.eternalcode.minions.config.ConfigService;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.behavior.MinionBehavior;
import com.eternalcode.minions.minion.MinionDirection;
import com.eternalcode.minions.minion.behavior.MaterialFilter;
import com.eternalcode.minions.minion.MinionPosition;
import com.eternalcode.minions.minion.behavior.MinionContext;
import com.eternalcode.minions.minion.behavior.MinionResult;
import com.eternalcode.minions.minion.storage.MinionItemTransferService;
import com.eternalcode.minions.minion.status.CoreMinionStatuses;
import com.eternalcode.minions.minion.tool.ToolCheck;
import com.eternalcode.minions.minion.tool.MinionToolPreparation;
import com.eternalcode.minions.minion.tool.MinionToolService;
import com.eternalcode.minions.minion.tool.ToolRequirement;
import java.util.Collection;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

public final class MiningBehavior implements MinionBehavior {

    private final MinerConfig config;
    private final MinionToolService tools;
    private final MinionItemTransferService transfers;
    private final ToolRequirement toolRequirement;
    private final MaterialFilter materialFilter;

    public static MiningBehavior create(
        ConfigService configs,
        MinionToolService tools,
        MinionItemTransferService transfers
    ) {
        MinerConfig config = configs.get(MinerConfig.class);
        return new MiningBehavior(config, tools, transfers);
    }

    public MiningBehavior(
        MinerConfig config,
        MinionToolService tools,
        MinionItemTransferService transfers
    ) {
        this.config = config;
        this.tools = tools;
        this.transfers = transfers;
        this.toolRequirement = config.toolRequirement();
        this.materialFilter = new MaterialFilter(
                config.materials(config.allowedMaterials),
                config.materials(config.blockedMaterials)
        );
        if (config.maxBlocksPerCycle < 0) {
            throw new IllegalArgumentException(
                    "miner.maxBlocksPerCycle cannot be negative: " + config.maxBlocksPerCycle
            );
        }
    }

    @Override
    public String id() {
        return "miner";
    }

    @Override
    public AbstractMinionConfig config() {
        return this.config;
    }

    @Override
    public MinionResult execute(MinionContext context) {
        MinionToolPreparation preparation = this.tools.prepare(
            context,
            this.toolRequirement,
            MinerStatuses.NO_PICKAXE
        );
        if (preparation.check() instanceof ToolCheck.Stopped stopped) {
            return MinionResult.idle(preparation.minion(), stopped.reason());
        }
        Minion minion = preparation.minion();

        MinionResult result = this.config.workMode == MinerConfig.WorkMode.LINE
            ? this.executeLinear(context, minion)
            : this.executeSquare(context, minion);
        if (result == null) {
            return MinionResult.idle(minion, MinerStatuses.NO_BLOCKS_IN_RANGE);
        }
        return result;
    }

    private MinionResult executeSquare(MinionContext context, Minion minion) {
        int radius = this.config.radius(minion.upgrades());
        int targetCount = MinionMiningTargets.count(radius);
        int workLimit = this.config.maxBlocksPerCycle == 0
                ? targetCount
                : Math.min(this.config.maxBlocksPerCycle, targetCount);
        int minedBlocks = 0;
        Minion updated = minion;

        for (int checkedTargets = 0; checkedTargets < targetCount; checkedTargets++) {
            int targetIndex = context.scheduledMinion().miningTargetIndex(targetCount);
            context.scheduledMinion().advanceMiningTarget(targetCount);
            int targetX = updated.position().blockX() + MinionMiningTargets.offsetX(radius, targetIndex);
            int targetY = updated.position().blockY() + MinionMiningTargets.offsetY();
            int targetZ = updated.position().blockZ() + MinionMiningTargets.offsetZ(radius, targetIndex);
            MinionResult result = this.tryMine(
                context,
                updated,
                targetX,
                targetY,
                targetZ,
                MinionMiningTargets.yaw(radius, targetIndex)
            );
            if (result == null) {
                continue;
            }
            if (!result.worked()) {
                return result;
            }
            updated = result.minion();
            minedBlocks++;
            if (minedBlocks >= workLimit) {
                break;
            }
        }
        return minedBlocks == 0 ? null : MinionResult.worked(updated, MinerStatuses.MINING);
    }

    private MinionResult executeLinear(MinionContext context, Minion minion) {
        MinionDirection direction = minion.settings().direction();
        int targetCount = 2 * this.config.radius(minion.upgrades()) + 1;
        int workLimit = this.config.maxBlocksPerCycle == 0
                ? targetCount
                : Math.min(this.config.maxBlocksPerCycle, targetCount);
        int minedBlocks = 0;
        Minion updated = minion;

        for (int checkedTargets = 0; checkedTargets < targetCount; checkedTargets++) {
            int distance = context.scheduledMinion().miningTargetIndex(targetCount) + 1;
            context.scheduledMinion().advanceMiningTarget(targetCount);

            int targetX = updated.position().blockX() + direction.offsetX() * distance;
            int targetY = updated.position().blockY();
            int targetZ = updated.position().blockZ() + direction.offsetZ() * distance;

            MinionResult result = this.tryMine(
                    context,
                    updated,
                    targetX,
                    targetY,
                    targetZ,
                    direction.yaw()
            );

            if (result == null) {
                continue;
            }
            if (!result.worked()) {
                return result;
            }
            updated = result.minion();
            minedBlocks++;
            if (minedBlocks >= workLimit) {
                break;
            }
        }

        return minedBlocks == 0 ? null : MinionResult.worked(updated, MinerStatuses.MINING);
    }

    private MinionResult tryMine(
        MinionContext context,
        Minion minion,
        int targetX,
        int targetY,
        int targetZ,
        float targetYaw
    ) {
        if (!context.world().isChunkLoaded(targetX >> 4, targetZ >> 4)) {
            return null;
        }

        Block block = context.world().getBlockAt(targetX, targetY, targetZ);
        if (!this.canMine(minion, block)) {
            return null;
        }

        ItemStack tool = minion.equipment().tool();
        ToolCheck toolCheck = this.tools.validateAgainstBlock(
            this.toolRequirement,
            tool,
            block,
            MinerStatuses.TOOL_TOO_WEAK
        );
        if (toolCheck instanceof ToolCheck.Stopped stopped) {
            return MinionResult.idle(minion, stopped.reason());
        }

        Collection<ItemStack> drops = tool == null ? block.getDrops() : block.getDrops(tool);
        if (!this.transfers.canStoreAll(context, minion.storage(), drops)) {
            return MinionResult.idle(minion, CoreMinionStatuses.STORAGE_FULL);
        }
        block.setType(Material.AIR, true);
        Minion updated = this.tools.consume(minion, 1);
        updated = this.transfers.deposit(context, updated, block.getLocation(), drops);
        updated = updated.withProgress(updated.progress().advanced(this.config));
        context.scheduledMinion().face(targetYaw);
        return MinionResult.worked(updated, MinerStatuses.MINING);
    }

    private boolean canMine(Minion minion, Block block) {
        Material material = block.getType();
        if (block.isEmpty() || block.isLiquid() || material == Material.BEDROCK) {
            return false;
        }
        if (!this.materialFilter.allows(material)) {
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
