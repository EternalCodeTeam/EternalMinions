package com.eternalcode.minions.minion;

import org.bukkit.inventory.ItemStack;

public final class MinionEquipment {

    private final ItemStack tool;
    private final long visualRevision;

    public MinionEquipment(ItemStack tool) {
        this(tool, 0L);
    }

    private MinionEquipment(ItemStack tool, long visualRevision) {
        this.tool = tool == null ? null : tool.clone();
        this.visualRevision = visualRevision;
    }

    public static MinionEquipment empty() {
        return new MinionEquipment(null);
    }

    public ItemStack tool() {
        return this.tool == null ? null : this.tool.clone();
    }

    public MinionEquipment withTool(ItemStack tool) {
        return new MinionEquipment(tool, this.visualRevision + 1L);
    }

    public MinionEquipment withDurability(ItemStack tool) {
        return new MinionEquipment(tool, this.visualRevision);
    }

    public boolean hasVisualChangeSince(MinionEquipment previous) {
        if (previous == null) {
            throw new IllegalArgumentException("Previous equipment is required");
        }

        return this.visualRevision != previous.visualRevision;
    }
}
