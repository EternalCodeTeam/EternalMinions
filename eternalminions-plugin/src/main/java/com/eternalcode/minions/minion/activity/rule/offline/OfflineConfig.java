package com.eternalcode.minions.minion.activity.rule.offline;

import com.eternalcode.minions.minion.activity.config.ActivityMode;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import java.util.LinkedHashMap;
import java.util.Map;

public class OfflineConfig extends OkaeriConfig {

    @Comment("Whether minions are nerfed while their owner is offline.")
    public boolean enabled = true;

    @Comment({
            "How long the owner can stay offline before the nerf kicks in, in seconds.",
            "Avoids punishing a minion for a short disconnect."
    })
    public int gracePeriodSeconds = 60;

    @Comment({
            "What happens once the grace period expires:",
            "FREEZE          - minion does nothing at all",
            "SLOW            - minion keeps working, but slower (see slow-multiplier)",
            "SLOW_NO_STORAGE - minion keeps working at slow-multiplier speed, but nothing produced is stored"
    })
    public ActivityMode mode = ActivityMode.FREEZE;

    @Comment({
            "Work speed multiplier used by SLOW and SLOW_NO_STORAGE.",
            "1.0 = full speed, 0.25 = quarter speed. Ignored by FREEZE."
    })
    public double slowMultiplier = 0.25D;

    @Comment({
            "Per minion-type overrides. Key is the minion type id (see /minions give suggestions).",
            "Only grace-period-seconds can be overridden here - anything omitted keeps the global value above."
    })
    public Map<String, OfflineOverride> overrides = new LinkedHashMap<>();

    public int gracePeriodSecondsFor(String minionTypeId) {
        OfflineOverride override = this.overrides.get(minionTypeId);
        if (override != null && override.gracePeriodSeconds != null) {
            return override.gracePeriodSeconds;
        }
        return this.gracePeriodSeconds;
    }

    public static class OfflineOverride extends OkaeriConfig {

        @Comment("Overrides offline.grace-period-seconds for this minion type. Empty = inherit.")
        public Integer gracePeriodSeconds = null;
    }
}
