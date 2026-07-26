package com.eternalcode.minions.config;

import com.cryptomorin.xseries.XMaterial;
import eu.okaeri.configs.OkaeriConfig;
import org.bukkit.Color;

public final class MinionItemsConfig extends OkaeriConfig {

    public MinionArmorPieceConfig helmet = new MinionArmorPieceConfig(XMaterial.PLAYER_HEAD);
    public MinionArmorPieceConfig chestplate = new MinionArmorPieceConfig(XMaterial.LEATHER_CHESTPLATE);
    public MinionArmorPieceConfig leggings = new MinionArmorPieceConfig(XMaterial.LEATHER_LEGGINGS);
    public MinionArmorPieceConfig boots = new MinionArmorPieceConfig(XMaterial.LEATHER_BOOTS);

    public void setLeatherArmorColor(Color color) {
        if (color == null) {
            throw new IllegalArgumentException("Leather armor color is required");
        }

        this.chestplate.color = color;
        this.leggings.color = color;
        this.boots.color = color;
    }
}
