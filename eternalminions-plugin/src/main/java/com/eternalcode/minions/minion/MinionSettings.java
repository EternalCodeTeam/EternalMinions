package com.eternalcode.minions.minion;

public record MinionSettings(MinionDirection direction, MiningMode miningMode) {

    public MinionSettings {
        if (direction == null || miningMode == null) {
            throw new IllegalArgumentException("Minion settings require direction and mining mode");
        }
    }

    public static MinionSettings defaults() {
        return new MinionSettings(MinionDirection.SOUTH, MiningMode.SQUARE);
    }

    public MinionSettings withDirection(MinionDirection direction) {
        return new MinionSettings(direction, this.miningMode);
    }

    public MinionSettings withMiningMode(MiningMode miningMode) {
        return new MinionSettings(this.direction, miningMode);
    }
}
