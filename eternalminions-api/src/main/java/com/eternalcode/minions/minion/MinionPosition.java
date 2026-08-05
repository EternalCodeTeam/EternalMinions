package com.eternalcode.minions.minion;

public record MinionPosition(String worldKey, int blockX, int blockY, int blockZ) {

    public MinionPosition {
        if (worldKey.isBlank()) {
            throw new IllegalArgumentException("World key must not be blank");
        }
    }
}
