package com.eternalcode.minions.minion.activity.rule.proximity;

import com.eternalcode.minions.minion.activity.config.ActivityMode;
import com.eternalcode.minions.minion.activity.config.PresenceMode;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import java.util.LinkedHashMap;
import java.util.Map;

public class ProximityConfig extends OkaeriConfig {

    @Comment("Whether minions are nerfed while nobody required is nearby.")
    public boolean enabled = true;

    @Comment("Distance in blocks between the minion and the required player(s).")
    public int radiusBlocks = 48;

    @Comment({
            "Who needs to be nearby to keep the minion at full activity:",
            "OWNER_ONLY  - only the minion's owner counts",
            "ANY_PLAYER  - any online player within range counts"
    })
    public PresenceMode presence = PresenceMode.OWNER_ONLY;

    @Comment("Same meaning as offline.mode, applied when nobody required is nearby.")
    public ActivityMode mode = ActivityMode.FREEZE;

    @Comment("Same meaning as offline.slow-multiplier, applied when nobody required is nearby.")
    public double slowMultiplier = 0.5D;

    @Comment({
            "If true and an earlier rule (e.g. offline) already nerfed the minion, skip this check",
            "entirely and keep that outcome. Set to false to evaluate proximity regardless",
            "(the more severe outcome still wins)."
    })
    public boolean skipIfOfflineRuleTriggered = true;

    @Comment({
            "Per minion-type overrides. Key is the minion type id (see /minions give suggestions).",
            "Only radius-blocks can be overridden here - anything omitted keeps the global value above."
    })
    public Map<String, ProximityOverride> overrides = new LinkedHashMap<>();

    public int radiusBlocksFor(String minionTypeId) {
        ProximityOverride override = this.overrides.get(minionTypeId);
        if (override != null && override.radiusBlocks != null) {
            return override.radiusBlocks;
        }
        return this.radiusBlocks;
    }

    public static class ProximityOverride extends OkaeriConfig {

        @Comment("Overrides proximity.radius-blocks for this minion type. Empty = inherit.")
        public Integer radiusBlocks = null;
    }
}
