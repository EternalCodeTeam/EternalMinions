package com.eternalcode.minions.minion;

public record MinionId(long value) {

    public MinionId {
        if (value <= 0L) {
            throw new IllegalArgumentException("Minion id must be positive");
        }
    }
}
