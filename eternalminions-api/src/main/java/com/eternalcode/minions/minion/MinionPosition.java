package com.eternalcode.minions.minion;

import java.util.Objects;

public record MinionPosition(String worldKey, int blockX, int blockY, int blockZ) {

    public MinionPosition {
        Objects.requireNonNull(worldKey, "worldKey");
        if (worldKey.isBlank()) {
            throw new IllegalArgumentException("World key must not be blank");
        }
    }
}
