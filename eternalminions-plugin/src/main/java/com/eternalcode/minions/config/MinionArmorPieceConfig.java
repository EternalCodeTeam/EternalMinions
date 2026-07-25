package com.eternalcode.minions.config;

import com.cryptomorin.xseries.XMaterial;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import org.bukkit.Color;

// One armor slot: `type` picks the material (use AIR to skip the slot). `texture` only applies to
// a PLAYER_HEAD type (base64 skin value, e.g. from minecraft-heads.com). `color`/`glow` only apply
// to leather pieces.
public final class MinionArmorPieceConfig extends OkaeriConfig {

    public XMaterial type;

    @Comment("PLAYER_HEAD only: base64 skin texture value.")
    public String texture = "";

    @Comment("Leather armor only: dye color.")
    public Color color = Color.WHITE;

    @Comment("Whether this piece has the enchantment glint effect.")
    public boolean glow = false;

    public MinionArmorPieceConfig() {
        this.type = XMaterial.AIR;
    }

    public MinionArmorPieceConfig(XMaterial type) {
        this.type = type;
    }
}
