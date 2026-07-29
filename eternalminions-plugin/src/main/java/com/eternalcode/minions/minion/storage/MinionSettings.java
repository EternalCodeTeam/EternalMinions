package com.eternalcode.minions.minion.storage;

import com.eternalcode.minions.minion.MinionDirection;

public record MinionSettings(MinionDirection direction) {

    public MinionSettings {
        if (direction == null) {
            throw new IllegalArgumentException("Minion settings require direction");
        }
    }

    public static MinionSettings defaults() {
        return new MinionSettings(MinionDirection.SOUTH);
    }

    public MinionSettings withDirection(MinionDirection direction) {
        return new MinionSettings(direction);
    }
}
