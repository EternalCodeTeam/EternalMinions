package com.eternalcode.minions.config;

import com.cryptomorin.xseries.XMaterial;
import eu.okaeri.configs.OkaeriConfig;

public final class MinionDropConfig extends OkaeriConfig {

    public XMaterial material = XMaterial.COBBLESTONE;
    public int minAmount = 1;
    public int maxAmount = 1;
    public double chance = 1.0;

    public MinionDropConfig() {
    }

    public MinionDropConfig(XMaterial material, int minAmount, int maxAmount, double chance) {
        this.material = material;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.chance = chance;
    }
}
