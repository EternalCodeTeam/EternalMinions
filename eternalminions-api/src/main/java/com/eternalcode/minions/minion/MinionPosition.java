package com.eternalcode.minions.minion;

import org.jetbrains.annotations.NotNull;

/** World namespaced key and exact block coordinates used by persistence-safe API operations. */
public record MinionPosition(@NotNull String worldKey, int blockX, int blockY, int blockZ) {

    public MinionPosition {
        if (worldKey == null || worldKey.isBlank()) {
            throw new IllegalArgumentException("World key must not be blank");
        }
    }
}
