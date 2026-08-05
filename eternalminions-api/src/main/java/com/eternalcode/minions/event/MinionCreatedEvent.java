package com.eternalcode.minions.event;

import com.eternalcode.minions.minion.MinionSnapshot;
import java.util.UUID;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Fired after a minion has been registered, rendered and queued for persistence. */
public final class MinionCreatedEvent extends MinionEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    private final MinionSnapshot minion;

    public MinionCreatedEvent(
        @NotNull MinionSnapshot minion,
        @NotNull MinionEventCause cause,
        @Nullable UUID actorId
    ) {
        super(cause, actorId);
        if (minion == null) {
            throw new IllegalArgumentException("Minion snapshot is required");
        }
        this.minion = minion;
    }

    public @NotNull MinionSnapshot minion() {
        return this.minion;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
