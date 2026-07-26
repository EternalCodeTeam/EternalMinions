package com.eternalcode.minions.minion;

import com.eternalcode.minions.minion.status.MinionStatus;

public record MinionResult(Minion minion, MinionStatus status, boolean worked, Long delayTicks) {

    public MinionResult {
        if (minion == null || status == null) {
            throw new IllegalArgumentException("Minion result requires minion and status");
        }
        if (delayTicks != null && delayTicks < 1L) {
            throw new IllegalArgumentException("Minion result delay must be positive");
        }
    }

    public static MinionResult idle(Minion minion, MinionStatus status) {
        return new MinionResult(minion, status, false, null);
    }

    public static MinionResult worked(Minion minion, MinionStatus status) {
        return new MinionResult(minion, status, true, null);
    }

    public MinionResult withDelay(long delayTicks) {
        return new MinionResult(this.minion, this.status, this.worked, delayTicks);
    }
}
