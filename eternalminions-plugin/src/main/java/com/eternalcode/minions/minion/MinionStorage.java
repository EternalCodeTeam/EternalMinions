package com.eternalcode.minions.minion;

import org.bukkit.inventory.ItemStack;

public final class MinionStorage {

    private final ItemStack[] items;

    public MinionStorage(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Storage capacity must be positive");
        }
        this.items = new ItemStack[capacity];
    }

    private MinionStorage(ItemStack[] items) {
        this.items = items;
    }

    public MinionStorageUpdate add(ItemStack input) {
        ItemStack[] updatedItems = this.items.clone();
        ItemStack remaining = input.clone();

        for (int slot = 0; slot < updatedItems.length; slot++) {
            ItemStack stored = updatedItems[slot];
            if (stored == null || !stored.isSimilar(remaining)) {
                continue;
            }
            int freeSpace = stored.getMaxStackSize() - stored.getAmount();
            if (freeSpace <= 0) {
                continue;
            }
            ItemStack updatedStack = stored.clone();
            int moved = Math.min(freeSpace, remaining.getAmount());
            updatedStack.setAmount(stored.getAmount() + moved);
            updatedItems[slot] = updatedStack;
            remaining.setAmount(remaining.getAmount() - moved);
            if (remaining.getAmount() == 0) {
                return new MinionStorageUpdate(new MinionStorage(updatedItems), null);
            }
        }

        for (int slot = 0; slot < updatedItems.length; slot++) {
            if (updatedItems[slot] != null) {
                continue;
            }
            int moved = Math.min(remaining.getMaxStackSize(), remaining.getAmount());
            ItemStack stored = remaining.clone();
            stored.setAmount(moved);
            updatedItems[slot] = stored;
            remaining.setAmount(remaining.getAmount() - moved);
            if (remaining.getAmount() == 0) {
                return new MinionStorageUpdate(new MinionStorage(updatedItems), null);
            }
        }

        return new MinionStorageUpdate(new MinionStorage(updatedItems), remaining);
    }

    public MinionStorage withItem(int slot, ItemStack item) {
        this.validateSlot(slot);
        ItemStack[] updatedItems = this.items.clone();
        updatedItems[slot] = item == null ? null : item.clone();
        return new MinionStorage(updatedItems);
    }

    public ItemStack item(int slot) {
        this.validateSlot(slot);
        ItemStack item = this.items[slot];
        return item == null ? null : item.clone();
    }

    public ItemStack[] snapshot() {
        ItemStack[] snapshot = new ItemStack[this.items.length];
        for (int slot = 0; slot < this.items.length; slot++) {
            ItemStack item = this.items[slot];
            snapshot[slot] = item == null ? null : item.clone();
        }
        return snapshot;
    }

    public int capacity() {
        return this.items.length;
    }

    private void validateSlot(int slot) {
        if (slot < 0 || slot >= this.items.length) {
            throw new IndexOutOfBoundsException("Storage slot " + slot + " is outside capacity " + this.items.length);
        }
    }
}
