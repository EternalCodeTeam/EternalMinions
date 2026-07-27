package com.eternalcode.minions.minion.tool;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

public final class EnchantmentLevels {

    private EnchantmentLevels() {
    }

    public static int level(ItemStack item, Enchantment enchantment) {
        if (item == null) {
            return 0;
        }

        return item.getEnchantmentLevel(enchantment);
    }
}
