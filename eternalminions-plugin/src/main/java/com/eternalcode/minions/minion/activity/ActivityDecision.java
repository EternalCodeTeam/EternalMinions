package com.eternalcode.minions.minion.activity;

import com.eternalcode.minions.minion.activity.config.ActivityMode;
import com.eternalcode.minions.minion.status.MinionStatus;

public record ActivityDecision(boolean frozen, double speedMultiplier, boolean suppressStorageGain,
                               MinionStatus statusOverride) {

    public static final ActivityDecision ACTIVE = new ActivityDecision(false, 1.0D, false, null);

    public ActivityDecision {
        if (!frozen && (!Double.isFinite(speedMultiplier) || speedMultiplier <= 0.0D)) {
            throw new IllegalArgumentException("Activity speed multiplier must be a positive, finite number");
        }
        if (!frozen) {
            speedMultiplier = Math.min(speedMultiplier, 1.0D);
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

    public ActivityDecision merge(ActivityDecision other) {
        if (this.frozen) {
            return this;
        }
        if (other.frozen) {
            return other;
        }
        if (this.active()) {
            return other;
        }
        if (other.active()) {
            return this;
        }

        ActivityDecision slower = this.speedMultiplier <= other.speedMultiplier ? this : other;
        boolean suppressStorageGain = this.suppressStorageGain || other.suppressStorageGain;
        return new ActivityDecision(false, slower.speedMultiplier, suppressStorageGain, slower.statusOverride);
    }

    public MinionExecutionPolicy executionPolicy() {
        return this.suppressStorageGain ? MinionExecutionPolicy.NO_STORAGE : MinionExecutionPolicy.FULL;
    }

    public boolean active() {
        return !this.frozen && this.speedMultiplier >= 1.0D && !this.suppressStorageGain;
    }
}
