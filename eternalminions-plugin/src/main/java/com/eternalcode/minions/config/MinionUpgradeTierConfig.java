package com.eternalcode.minions.config;

import com.cryptomorin.xseries.XMaterial;
import eu.okaeri.configs.OkaeriConfig;

public final class MinionUpgradeTierConfig extends OkaeriConfig {

    public int requiredLevel = 1;
    public int value = 1;
    public XMaterial costMaterial = XMaterial.DIAMOND;
    public int costAmount = 8;

    public MinionUpgradeTierConfig() {
    }

    public MinionUpgradeTierConfig(int requiredLevel, int value, XMaterial costMaterial, int costAmount) {
        this.requiredLevel = requiredLevel;
        this.value = value;
        this.costMaterial = costMaterial;
        this.costAmount = costAmount;
    }
}
