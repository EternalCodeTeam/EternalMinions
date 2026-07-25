package com.eternalcode.minions.config;

import com.cryptomorin.xseries.XMaterial;
import com.eternalcode.minions.minion.tool.ToolCategory;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import java.util.List;

public final class MinionToolConfig extends OkaeriConfig {

    @Comment("Required tool category for this profession.")
    public ToolCategory category = ToolCategory.NONE;

    @Comment("If not empty, only these exact materials satisfy the tool requirement.")
    public List<XMaterial> allowedMaterials = List.of();

    @Comment("Remaining durability at which the tool is retired instead of being destroyed.")
    public int minDurabilityToKeep = 1;

    @Comment("Whether this profession must have a matching tool before it can work.")
    public boolean required = false;
}
