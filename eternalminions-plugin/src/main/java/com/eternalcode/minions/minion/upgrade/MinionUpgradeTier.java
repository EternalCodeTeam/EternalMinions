package com.eternalcode.minions.minion.upgrade;

import java.math.BigDecimal;

public record MinionUpgradeTier(int requiredLevel, int value, BigDecimal costAmount) {

    public MinionUpgradeTier {
        if (requiredLevel < 1 || value < 1 || costAmount == null || costAmount.signum() <= 0) {
            throw new IllegalArgumentException("Upgrade tier requires positive level, value and cost");
        }
    }
}
