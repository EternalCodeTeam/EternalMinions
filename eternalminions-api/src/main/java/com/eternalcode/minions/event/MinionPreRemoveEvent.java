package com.eternalcode.minions.event;

import com.eternalcode.minions.minion.MinionSnapshot;
import java.util.UUID;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Fired before a live minion is removed. Cancelling leaves the minion unchanged. */
public final class MinionPreRemoveEvent extends MinionEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final MinionSnapshot minion;
    private boolean cancelled;

    public MinionPreRemoveEvent(
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
