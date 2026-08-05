package com.eternalcode.minions.event;

import com.eternalcode.minions.minion.MinionCreateRequest;
import java.util.UUID;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Fired before a minion is registered, rendered and persisted. Cancelling prevents creation. */
public final class MinionPreCreateEvent extends MinionEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final MinionCreateRequest request;
    private boolean cancelled;

    public MinionPreCreateEvent(
        @NotNull MinionCreateRequest request,
        @NotNull MinionEventCause cause,
        @Nullable UUID actorId
    ) {
        super(cause, actorId);
        if (request == null) {
            throw new IllegalArgumentException("Create request is required");
        }
        this.request = request;
    }

    public @NotNull MinionCreateRequest request() {
        return this.request;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
