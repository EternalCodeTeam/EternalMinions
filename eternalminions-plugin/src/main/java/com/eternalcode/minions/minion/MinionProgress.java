package com.eternalcode.minions.minion;

public record MinionProgress(int level) {

    public MinionProgress {
        if (level < 1) {
            throw new IllegalArgumentException("Minion level must be positive");
        }
    }
}
