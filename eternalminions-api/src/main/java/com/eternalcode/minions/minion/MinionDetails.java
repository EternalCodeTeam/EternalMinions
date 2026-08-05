package com.eternalcode.minions.minion;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/** Lightweight immutable identity, ownership, type, position and level view of a minion. */
public record MinionDetails(
    @NotNull MinionId id,
    @NotNull UUID ownerId,
    @NotNull String behaviorId,
    @NotNull MinionPosition position,
    int level
) {

    public MinionDetails {
        if (id == null || ownerId == null || position == null) {
            throw new IllegalArgumentException("Minion identity and position are required");
        }
        if (behaviorId == null || behaviorId.isBlank()) {
            throw new IllegalArgumentException("Behavior id must not be blank");
        }
        if (level < 1) {
            throw new IllegalArgumentException("Minion level must be positive");
        }
    }
}
