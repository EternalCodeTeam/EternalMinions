package com.eternalcode.minions.minion;

import java.util.Objects;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

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
        if (Objects.equals(this.tool, tool)) {
            return this;
        }

        return new MinionEquipment(tool, this.visualRevision);
    }

    public int toolDamage() {
        if (this.tool == null) {
            return -1;
        }

        ItemMeta itemMeta = this.tool.getItemMeta();
        return itemMeta instanceof Damageable damageable ? damageable.getDamage() : -1;
    }

    public boolean hasVisualChangeSince(MinionEquipment previous) {
        if (previous == null) {
            throw new IllegalArgumentException("Previous equipment is required");
        }

        return this.visualRevision != previous.visualRevision;
    }
}
