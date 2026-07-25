package com.eternalcode.minions.minion.tool;

import com.eternalcode.minions.minion.status.CoreMinionStatuses;
import com.eternalcode.minions.minion.status.MinionStatus;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

public final class ToolValidationService {

    public ToolCheck validate(ToolRequirement requirement, ItemStack tool) {
        return this.validate(requirement, tool, CoreMinionStatuses.NO_TOOL);
    }

    // Overload accepting a profession-specific "missing tool" status (e.g. "Brak kilofa" for
    // MINER instead of the generic "Brak narzędzia"), so the hologram reports the exact reason.
    public ToolCheck validate(ToolRequirement requirement, ItemStack tool, MinionStatus missingToolStatus) {
        if (requirement == null) {
            throw new IllegalArgumentException("Tool requirement must not be null");
        }
        if (missingToolStatus == null) {
            throw new IllegalArgumentException("Missing tool status must not be null");
        }
        if (tool == null || tool.getType() == Material.AIR) {
            return requirement.required()
                ? new ToolCheck.Stopped(missingToolStatus)
                : ToolCheck.Ready.EMPTY;
        }
        if (!requirement.matches(tool.getType())) {
            return new ToolCheck.Stopped(missingToolStatus);
        }
        if (this.remainingDurability(tool) < requirement.minDurabilityToKeep()) {
            return new ToolCheck.Stopped(missingToolStatus);
        }

        return new ToolCheck.Ready(tool);
    }

    public ToolCheck validateMaterial(ToolRequirement requirement, Material material) {
        if (requirement == null) {
            throw new IllegalArgumentException("Tool requirement must not be null");
        }
        if (material == null || material == Material.AIR) {
            return requirement.required()
                ? new ToolCheck.Stopped(CoreMinionStatuses.NO_TOOL)
                : ToolCheck.Ready.EMPTY;
        }
        if (!requirement.matches(material)) {
            return new ToolCheck.Stopped(CoreMinionStatuses.NO_TOOL);
        }

        return ToolCheck.Ready.EMPTY;
    }

    public ToolCheck validateAgainstBlock(
        ToolRequirement requirement,
        ItemStack tool,
        Block target,
        MinionStatus weakToolStatus
    ) {
        if (target == null) {
            throw new IllegalArgumentException("Tool target block must not be null");
        }
        if (weakToolStatus == null) {
            throw new IllegalArgumentException("Weak tool status must not be null");
        }

        ToolCheck baseCheck = this.validate(requirement, tool);
        if (!(baseCheck instanceof ToolCheck.Ready ready)) {
            return baseCheck;
        }
        ItemStack readyTool = ready.tool();
        if (readyTool == null || !target.getBlockData().isPreferredTool(readyTool)) {
            return new ToolCheck.Stopped(weakToolStatus);
        }

        return baseCheck;
    }

    public int remainingDurability(ItemStack tool) {
        if (tool == null) {
            throw new IllegalArgumentException("Tool item must not be null");
        }

        short maxDurability = tool.getType().getMaxDurability();
        if (maxDurability <= 0) {
            return Integer.MAX_VALUE;
        }

        ItemMeta meta = tool.getItemMeta();
        if (!(meta instanceof Damageable damageable)) {
            return maxDurability;
        }

        return maxDurability - damageable.getDamage();
    }
}
