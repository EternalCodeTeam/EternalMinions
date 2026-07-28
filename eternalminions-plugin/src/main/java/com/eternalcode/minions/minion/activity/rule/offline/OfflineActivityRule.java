package com.eternalcode.minions.minion.activity.rule.offline;

import com.eternalcode.minions.minion.activity.ActivityDecision;
import com.eternalcode.minions.minion.activity.MinionActivityContext;
import com.eternalcode.minions.minion.activity.rule.MinionActivityRule;
import com.eternalcode.minions.minion.status.CoreMinionStatuses;

public final class OfflineActivityRule implements MinionActivityRule {

    private final OfflineConfig config;

    public OfflineActivityRule(OfflineConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Offline rule config is required");
        }
        this.config = config;
    }

    @Override
    public ActivityDecision evaluate(MinionActivityContext context) {
        if (!this.config.enabled || context.owner() != null) {
            return ActivityDecision.ACTIVE;
        }

        int gracePeriodSeconds = this.config.gracePeriodSecondsFor(context.minion().behaviorId());
        long lastSeenMillis = context.server().getOfflinePlayer(context.minion().ownerId()).getLastSeen();
        long offlineMillis = context.currentTimeMillis() - lastSeenMillis;
        if (offlineMillis < gracePeriodSeconds * 1000L) {
            return ActivityDecision.ACTIVE;
        }

        return ActivityDecision.of(this.config.mode, this.config.slowMultiplier, CoreMinionStatuses.OFFLINE);
    }
}
