package com.eternalcode.minions.minion.tool;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class ToolInventoryLocator {

    public int findInMaterials(Material[] materials, ToolRequirement requirement) {
        if (materials == null) {
            throw new IllegalArgumentException("Tool materials must not be null");
        }
        if (requirement == null) {
            throw new IllegalArgumentException("Tool requirement must not be null");
        }

        for (int slot = 0; slot < materials.length; slot++) {
            Material material = materials[slot];
            if (material != null && requirement.matches(material)) {
                return slot;
            }
        }
        return -1;
    }

    public int findInStorage(ItemStack[] storage, ToolRequirement requirement) {
        if (storage == null) {
            throw new IllegalArgumentException("Tool storage contents must not be null");
        }
        if (requirement == null) {
            throw new IllegalArgumentException("Tool requirement must not be null");
        }

        for (int slot = 0; slot < storage.length; slot++) {
            ItemStack item = storage[slot];
            if (item != null && requirement.matches(item.getType())) {
                return slot;
            }
        }
        return -1;
    }

    public int findInInventory(Inventory inventory, ToolRequirement requirement) {
        if (inventory == null) {
            throw new IllegalArgumentException("Tool inventory must not be null");
        }

        return this.findInStorage(inventory.getContents(), requirement);
    }
}
