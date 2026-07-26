package com.eternalcode.minions.minion;

import com.eternalcode.minions.config.AbstractMinionConfig;

public record MinionProgress(int level, long progress) {

    public MinionProgress {
        if (level < 1) {
            throw new IllegalArgumentException("Minion level must be positive");
        }
        if (progress < 0) {
            throw new IllegalArgumentException("Minion progress cannot be negative");
        }
    }

    public static MinionProgress start() {
        return new MinionProgress(1, 0L);
    }

    public MinionProgress advanced(AbstractMinionConfig config) {
        long updatedProgress = this.progress + 1;
        int updatedLevel = this.level;
        while (updatedLevel < config.maxLevel() && updatedProgress >= config.progressToReach(updatedLevel + 1)) {
            updatedLevel++;
        }
        return new MinionProgress(updatedLevel, updatedProgress);
    }
}
