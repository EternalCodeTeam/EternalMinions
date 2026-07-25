package com.eternalcode.minions.minion;

import org.bukkit.inventory.ItemStack;

public final class MinionEquipment {

    private final ItemStack tool;

    public MinionEquipment(ItemStack tool) {
        this.tool = tool == null ? null : tool.clone();
    }

    public static MinionEquipment empty() {
        return new MinionEquipment(null);
    }

    public ItemStack tool() {
        return this.tool == null ? null : this.tool.clone();
    }

    public MinionEquipment withTool(ItemStack tool) {
        return new MinionEquipment(tool);
    }
}
