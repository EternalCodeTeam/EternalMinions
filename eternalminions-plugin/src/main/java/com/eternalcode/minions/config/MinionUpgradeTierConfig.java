package com.eternalcode.minions.config;

import eu.okaeri.configs.OkaeriConfig;
import java.math.BigDecimal;

public final class MinionUpgradeTierConfig extends OkaeriConfig {

    public int requiredLevel = 1;
    public int value = 1;
    public BigDecimal costAmount = new BigDecimal("100.00");

    public MinionUpgradeTierConfig() {
    }

    public MinionUpgradeTierConfig(int requiredLevel, int value, BigDecimal costAmount) {
        this.requiredLevel = requiredLevel;
        this.value = value;
        this.costAmount = costAmount;
    }
}
