package com.eternalcode.minions.minion.activity;

import com.eternalcode.minions.minion.activity.config.ActivityMode;
import com.eternalcode.minions.minion.status.MinionStatus;

public record ActivityDecision(boolean frozen, double speedMultiplier, boolean suppressStorageGain,
                               MinionStatus statusOverride) {

    public static final ActivityDecision ACTIVE = new ActivityDecision(false, 1.0D, false, null);

    public ActivityDecision {
        if (!frozen) {
            if (Double.isNaN(speedMultiplier) || Double.isInfinite(speedMultiplier) || speedMultiplier <= 0.0D) {
                throw new IllegalArgumentException("Activity speed multiplier must be a positive, finite number");
            }
            if (speedMultiplier > 1.0D) {
                speedMultiplier = 1.0D;
            }
        }
    }

    public static ActivityDecision frozen(MinionStatus status) {
        return new ActivityDecision(true, 0.0D, true, status);
    }

    public static ActivityDecision of(ActivityMode mode, double slowMultiplier, MinionStatus status) {
        return switch (mode) {
            case FREEZE -> frozen(status);
            case SLOW -> new ActivityDecision(false, slowMultiplier, false, status);
            case SLOW_NO_STORAGE -> new ActivityDecision(false, slowMultiplier, true, status);
        };
    }

    public static ActivityDecision moreSevere(ActivityDecision first, ActivityDecision second) {
        if (first.frozen) {
            return first;
        }
        if (second.frozen) {
            return second;
        }
        if (first.active()) {
            return second;
        }
        if (second.active()) {
            return first;
        }

        ActivityDecision slower = first.speedMultiplier <= second.speedMultiplier ? first : second;
        boolean suppressStorageGain = first.suppressStorageGain || second.suppressStorageGain;
        return new ActivityDecision(false, slower.speedMultiplier, suppressStorageGain, slower.statusOverride);
    }

    public boolean active() {
        return !this.frozen && this.speedMultiplier >= 1.0D && !this.suppressStorageGain;
    }
}
