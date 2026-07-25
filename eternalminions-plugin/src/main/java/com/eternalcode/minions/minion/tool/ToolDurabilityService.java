package com.eternalcode.minions.minion.tool;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

public final class ToolDurabilityService {

    static boolean shouldApplyDamage(int unbreakingLevel, Random random) {
        if (unbreakingLevel < 0) {
            throw new IllegalArgumentException("Unbreaking level must not be negative");
        }
        if (random == null) {
            throw new IllegalArgumentException("Random must not be null");
        }

        return random.nextInt(unbreakingLevel + 1) == 0;
    }

    public ItemStack consume(ItemStack tool, int amount) {
        if (tool == null) {
            throw new IllegalArgumentException("Tool item must not be null");
        }
        if (amount < 1) {
            throw new IllegalArgumentException("Durability amount must be positive");
        }

        ItemMeta meta = tool.getItemMeta();
        if (!(meta instanceof Damageable)) {
            return tool.clone();
        }

        int unbreakingLevel = tool.getEnchantmentLevel(Enchantment.UNBREAKING);
        int appliedDamage = this.appliedDamage(amount, unbreakingLevel);
        if (appliedDamage == 0) {
            return tool.clone();
        }

        ItemStack damagedTool = tool.clone();
        Damageable damagedMeta = (Damageable) damagedTool.getItemMeta();
        damagedMeta.setDamage(damagedMeta.getDamage() + appliedDamage);
        damagedTool.setItemMeta(damagedMeta);
        return damagedTool;
    }

    private int appliedDamage(int amount, int unbreakingLevel) {
        Random random = ThreadLocalRandom.current();
        int appliedDamage = 0;

        for (int use = 0; use < amount; use++) {
            if (shouldApplyDamage(unbreakingLevel, random)) {
                appliedDamage++;
            }
        }

        return appliedDamage;
    }
}
