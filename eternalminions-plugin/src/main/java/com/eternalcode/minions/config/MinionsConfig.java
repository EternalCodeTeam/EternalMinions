package com.eternalcode.minions.config;

import com.eternalcode.minions.render.MinionRendererType;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;

public final class MinionsConfig extends OkaeriConfig {

    @Comment("Renderer used by every minion. Changing it requires a server restart.")
    public MinionRendererType minionRenderer = MinionRendererType.ARMOR_STAND;

    @Comment("Maximum distance at which a client-side minion representation is spawned.")
    public int renderDistanceBlocks = 64;

    @Comment("Maximum distance at which action animations and particles are sent.")
    public int animationDistanceBlocks = 32;

    @Comment("Maximum scheduler work budget during one server tick, in microseconds.")
    public int schedulerBudgetMicros = 2_000;

    @Comment("Maximum number of physical world actions executed during one server tick.")
    public int physicalActionsPerTick = 250;

    @Comment("Maximum amount of offline progress credited after loading a minion.")
    public int maximumOfflineHours = 168;
}
