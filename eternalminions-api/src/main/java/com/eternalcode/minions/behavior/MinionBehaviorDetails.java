package com.eternalcode.minions.behavior;

import org.jetbrains.annotations.NotNull;

/** Public description of an enabled minion behavior. */
public record MinionBehaviorDetails(@NotNull String id) {

    public MinionBehaviorDetails {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Behavior id must not be blank");
        }
    }
}
