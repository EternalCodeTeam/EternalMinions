package com.eternalcode.minions.minion.upgrade;

import org.bukkit.Material;

public record MinionUpgradeTier(int requiredLevel, int value, Material costMaterial, int costAmount) {

    public MinionUpgradeTier {
        if (requiredLevel < 1 || value < 1 || costAmount < 1) {
            throw new IllegalArgumentException("Upgrade tier requires positive level, value and cost");
        }
        if (costMaterial == null) {
            throw new IllegalArgumentException("Upgrade tier requires a cost material");
        }
    }
}
