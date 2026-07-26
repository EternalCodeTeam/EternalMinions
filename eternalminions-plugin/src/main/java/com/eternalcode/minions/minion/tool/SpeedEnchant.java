package com.eternalcode.minions.minion.tool;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

// Simple, non-realistic speed scaling from a tool's Efficiency level: each level shortens the
// minion's next action interval by 25% (floor 10% of the base interval, minimum 1 tick). Gated
// The profession config decides whether this scaling is used.
public final class SpeedEnchant {

    private static final double REDUCTION_PER_LEVEL = 0.25D;
    private static final double MIN_FACTOR = 0.1D;

    private SpeedEnchant() {
    }

    public static long scaledInterval(long baseIntervalTicks, ItemStack tool) {
        if (tool == null) {
            return baseIntervalTicks;
        }
        int level = tool.getEnchantmentLevel(Enchantment.EFFICIENCY);
        if (level <= 0) {
            return baseIntervalTicks;
        }
        double factor = Math.max(MIN_FACTOR, 1.0D - REDUCTION_PER_LEVEL * level);
        return Math.max(1L, Math.round(baseIntervalTicks * factor));
    }
}
