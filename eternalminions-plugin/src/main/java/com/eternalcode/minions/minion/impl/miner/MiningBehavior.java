package com.eternalcode.minions.minion.impl.miner;

import com.eternalcode.minions.config.AbstractMinionConfig;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionBehavior;
import com.eternalcode.minions.minion.MinionContext;
import com.eternalcode.minions.minion.MinionDirection;
import com.eternalcode.minions.minion.MinionPosition;
import com.eternalcode.minions.minion.MinionResult;
import com.eternalcode.minions.minion.MiningMode;
import com.eternalcode.minions.minion.tool.SpeedEnchant;
import com.eternalcode.minions.minion.tool.ToolCheck;
import com.eternalcode.minions.minion.tool.ToolDurabilityService;
import com.eternalcode.minions.minion.tool.ToolInventoryLocator;
import com.eternalcode.minions.minion.tool.ToolRequirement;
import com.eternalcode.minions.minion.tool.ToolValidationService;
import java.util.Collection;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

public final class MiningBehavior implements MinionBehavior {

    private final MinerConfig config;
    private final ToolValidationService toolValidation;
    private final ToolDurabilityService toolDurability;
    private final ToolInventoryLocator toolLocator;
    private final ToolRequirement toolRequirement;
    private final Set<Material> blockedMaterials;
    private final Set<Material> allowedMaterials;

    public MiningBehavior(
        MinerConfig config,
        ToolValidationService toolValidation,
        ToolDurabilityService toolDurability,
        ToolInventoryLocator toolLocator
    ) {
        this.config = config;
        this.toolValidation = toolValidation;
        this.toolDurability = toolDurability;
        this.toolLocator = toolLocator;
        this.toolRequirement = config.toolRequirement();
        this.blockedMaterials = config.materials(config.blockedMaterials);
        this.allowedMaterials = config.materials(config.allowedMaterials);
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
        Minion minion = context.retireWornTool(
            context.minion(),
            this.toolRequirement,
            this.toolValidation,
            this.toolLocator
        );

        ToolCheck toolCheck = this.toolValidation.validate(
            this.toolRequirement,
            minion.equipment().tool(),
            MinerStatuses.NO_PICKAXE
        );
        if (toolCheck instanceof ToolCheck.Stopped stopped) {
            return MinionResult.idle(minion, stopped.reason());
        }

        MinionResult result = minion.settings().miningMode() == MiningMode.LINEAR
            ? this.executeLinear(context, minion)
            : this.executeSquare(context, minion);
        if (result == null) {
            return MinionResult.idle(minion, MinerStatuses.NO_BLOCKS_IN_RANGE);
        }
        if (!result.worked() || !this.config.respectSpeedEnchants) {
            return result;
        }

        long baseInterval = this.config.workInterval(result.minion().upgrades());
        long delay = SpeedEnchant.scaledInterval(baseInterval, result.minion().equipment().tool());
        return result.withDelay(delay);
    }

    private MinionResult executeSquare(MinionContext context, Minion minion) {
        int radius = this.config.radius(minion.upgrades());
        int targetCount = MinionMiningTargets.count(radius);
        for (int checkedTargets = 0; checkedTargets < targetCount; checkedTargets++) {
            int targetIndex = context.scheduledMinion().miningTargetIndex(targetCount);
            context.scheduledMinion().advanceMiningTarget(targetCount);
            int targetX = minion.position().blockX() + MinionMiningTargets.offsetX(radius, targetIndex);
            int targetY = minion.position().blockY() + MinionMiningTargets.offsetY();
            int targetZ = minion.position().blockZ() + MinionMiningTargets.offsetZ(radius, targetIndex);
            MinionResult result = this.tryMine(
                context,
                minion,
                targetX,
                targetY,
                targetZ,
                MinionMiningTargets.yaw(radius, targetIndex)
            );
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private MinionResult executeLinear(MinionContext context, Minion minion) {
        MinionDirection direction = minion.settings().direction();
        int targetCount = 2 * this.config.radius(minion.upgrades()) + 1;
        for (int checkedTargets = 0; checkedTargets < targetCount; checkedTargets++) {
            int distance = context.scheduledMinion().miningTargetIndex(targetCount) + 1;
            context.scheduledMinion().advanceMiningTarget(targetCount);
            int targetX = minion.position().blockX() + direction.offsetX() * distance;
            int targetY = minion.position().blockY() + MinionMiningTargets.offsetY();
            int targetZ = minion.position().blockZ() + direction.offsetZ() * distance;
            MinionResult result = this.tryMine(context, minion, targetX, targetY, targetZ, direction.yaw());
            if (result != null) {
                return result;
            }
        }
        return null;
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
        ToolCheck toolCheck = this.toolValidation.validateAgainstBlock(
            this.toolRequirement,
            tool,
            block,
            MinerStatuses.TOOL_TOO_WEAK
        );
        if (toolCheck instanceof ToolCheck.Stopped stopped) {
            return MinionResult.idle(minion, stopped.reason());
        }

        Collection<ItemStack> drops = tool == null ? block.getDrops() : block.getDrops(tool);
        block.setType(Material.AIR, true);
        Minion updated = this.consumeTool(minion, tool);
        updated = context.deposit(updated, block.getLocation(), drops);
        updated = updated.withProgress(updated.progress().advanced(this.config));
        context.scheduledMinion().face(targetYaw);
        return MinionResult.worked(updated, MinerStatuses.MINING);
    }

    private Minion consumeTool(Minion minion, ItemStack tool) {
        if (tool == null) {
            return minion;
        }

        ItemStack damagedTool = this.toolDurability.consume(tool, 1);
        return minion.withEquipment(minion.equipment().withTool(damagedTool));
    }

    private boolean canMine(Minion minion, Block block) {
        Material material = block.getType();
        if (block.isEmpty() || block.isLiquid() || material == Material.BEDROCK) {
            return false;
        }
        if (this.blockedMaterials.contains(material)) {
            return false;
        }
        if (!this.allowedMaterials.isEmpty() && !this.allowedMaterials.contains(material)) {
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
