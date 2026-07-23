package com.eternalcode.minions.config;

import com.cryptomorin.xseries.XMaterial;
import eu.okaeri.configs.OkaeriConfig;
import java.util.List;

public final class MinionPanelElementConfig extends OkaeriConfig {

    public MinionPanelAction action = MinionPanelAction.NONE;
    public XMaterial material = XMaterial.STONE;
    public int amount = 1;
    public String displayName = " ";
    public List<String> lore = List.of();
    public boolean glowing;
    public int customModelData;
    public boolean hideTooltip;
}
