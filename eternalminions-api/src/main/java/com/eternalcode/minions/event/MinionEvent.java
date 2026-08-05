package com.eternalcode.minions.event;

import java.util.UUID;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Base class for all EternalMinions Bukkit events. Events are always fired on the server thread. */
public abstract class MinionEvent extends Event {

    private final MinionEventCause cause;
    private final UUID actorId;

    protected MinionEvent(@NotNull MinionEventCause cause, @Nullable UUID actorId) {
        if (cause == null) {
            throw new IllegalArgumentException("Event cause is required");
        }
        this.cause = cause;
        this.actorId = actorId;
    }

    /** Returns why the operation occurred. */
    public final @NotNull MinionEventCause cause() {
        return this.cause;
    }

    /** Returns the responsible player UUID, or {@code null} for non-player operations. */
    public final @Nullable UUID actorId() {
        return this.actorId;
    }
}
