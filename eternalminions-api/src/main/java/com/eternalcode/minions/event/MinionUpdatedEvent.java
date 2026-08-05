package com.eternalcode.minions.event;

import com.eternalcode.minions.minion.MinionSnapshot;
import java.util.UUID;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Fired after public minion state changes, with immutable state from before and after the update. */
public final class MinionUpdatedEvent extends MinionEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    private final MinionSnapshot previous;
    private final MinionSnapshot current;
    private final MinionUpdateType updateType;

    public MinionUpdatedEvent(
        @NotNull MinionSnapshot previous,
        @NotNull MinionSnapshot current,
        @NotNull MinionUpdateType updateType,
        @NotNull MinionEventCause cause,
        @Nullable UUID actorId
    ) {
        super(cause, actorId);
        if (previous == null || current == null || updateType == null) {
            throw new IllegalArgumentException("Previous state, current state and update type are required");
        }
        this.previous = previous;
        this.current = current;
        this.updateType = updateType;
    }

    public @NotNull MinionSnapshot previous() {
        return this.previous;
    }

    public @NotNull MinionSnapshot current() {
        return this.current;
    }

    public @NotNull MinionUpdateType updateType() {
        return this.updateType;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
