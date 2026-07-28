package com.eternalcode.minions.minion.activity.rule.loadedchunk;

import com.eternalcode.minions.minion.MinionPosition;
import com.eternalcode.minions.minion.activity.ActivityDecision;
import com.eternalcode.minions.minion.activity.MinionActivityContext;
import com.eternalcode.minions.minion.activity.rule.MinionActivityRule;
import com.eternalcode.minions.minion.status.CoreMinionStatuses;

public final class LoadedChunkActivityRule implements MinionActivityRule {

    private final LoadedChunkConfig config;

    public LoadedChunkActivityRule(LoadedChunkConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Loaded chunk rule config is required");
        }
        this.config = config;
    }

    @Override
    public ActivityDecision evaluate(MinionActivityContext context) {
        if (!this.config.enabled) {
            return ActivityDecision.ACTIVE;
        }

        MinionPosition position = context.minion().position();
        if (context.world().isChunkLoaded(position.blockX() >> 4, position.blockZ() >> 4)) {
            return ActivityDecision.ACTIVE;
        }

        return ActivityDecision.frozen(CoreMinionStatuses.IDLE);
    }
}
