package com.eternalcode.minions.minion.impl.farmer;

import org.bukkit.Material;

// How a harvested crop block behaves structurally, so FarmerBehavior never breaks the stem
// (pumpkin/melon) or the bottom block (sugar cane/bamboo) the way real farming never does.
public enum CropShape {

    AGEABLE_REPLANT,
    STACKING_COLUMN,
    ADJACENT_STEM_FRUIT;

    public static CropShape of(Material material) {
        if (material == Material.SUGAR_CANE || material == Material.BAMBOO) {
            return STACKING_COLUMN;
        }
        if (material == Material.PUMPKIN || material == Material.MELON) {
            return ADJACENT_STEM_FRUIT;
        }
        return AGEABLE_REPLANT;
    }
}
