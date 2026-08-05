package com.eternalcode.minions.minion;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/** Input required to create a fresh live minion through the management API. */
public record MinionCreateRequest(
    @NotNull UUID ownerId,
    @NotNull String behaviorId,
    @NotNull MinionPosition position,
    @NotNull MinionDirection direction
) {

    public MinionCreateRequest {
        if (ownerId == null || position == null || direction == null) {
            throw new IllegalArgumentException("Owner, position and direction are required");
        }
        if (behaviorId == null || behaviorId.isBlank()) {
            throw new IllegalArgumentException("Behavior id must not be blank");
        }
    }
}
