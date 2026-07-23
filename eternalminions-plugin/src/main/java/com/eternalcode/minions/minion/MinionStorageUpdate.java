package com.eternalcode.minions.minion;

import org.bukkit.inventory.ItemStack;

public record MinionStorageUpdate(MinionStorage storage, ItemStack remaining) {

    public MinionStorageUpdate {
        if (storage == null) {
            throw new IllegalArgumentException("Updated storage is required");
        }
        remaining = remaining == null ? null : remaining.clone();
    }

    @Override
    public ItemStack remaining() {
        return this.remaining == null ? null : this.remaining.clone();
    }
}
