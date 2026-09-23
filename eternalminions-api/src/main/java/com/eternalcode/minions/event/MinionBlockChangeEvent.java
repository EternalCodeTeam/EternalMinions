package com.eternalcode.minions.event;

import com.eternalcode.minions.minion.MinionSnapshot;
import java.util.function.Supplier;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired before a working minion changes a block in the world. The block still holds its old state
 * while listeners run. Cancelling makes the minion skip this target; for a tree the whole tree is skipped.
 * Fired per block, so listeners should stay cheap.
 */
public final class MinionBlockChangeEvent extends MinionEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Supplier<MinionSnapshot> minionSupplier;
    private final Block block;
    private final Material material;
    private final Material newMaterial;
    private final MinionBlockChangeReason reason;
    private MinionSnapshot minion;
    private boolean cancelled;

    public MinionBlockChangeEvent(
        @NotNull Supplier<MinionSnapshot> minion,
        @NotNull Block block,
        @NotNull Material newMaterial,
        @NotNull MinionBlockChangeReason reason
    ) {
        super(MinionEventCause.INTERNAL, null);
        if (minion == null || block == null || newMaterial == null || reason == null) {
            throw new IllegalArgumentException("Minion, block, new material and reason are required");
        }
        this.minionSupplier = minion;
        this.block = block;
        this.material = block.getType();
        this.newMaterial = newMaterial;
        this.reason = reason;
    }

    /** Returns the working minion. The snapshot is built on first access and cached. */
    public @NotNull MinionSnapshot minion() {
        if (this.minion == null) {
            this.minion = this.minionSupplier.get();
        }
        return this.minion;
    }

    /** Returns the block about to change. It still holds its pre-change state. */
    public @NotNull Block block() {
        return this.block;
    }

    /** Returns the block material before the change. */
    public @NotNull Material material() {
        return this.material;
    }

    /**
     * Returns the material the block will hold after the change: {@link Material#AIR} for a removed block,
     * a sapling for a replanted log, or the same material for a crop reset to age zero.
     */
    public @NotNull Material newMaterial() {
        return this.newMaterial;
    }

    public @NotNull MinionBlockChangeReason reason() {
        return this.reason;
    }

    /** Returns whether any listener is registered, letting callers skip building the event entirely. */
    public static boolean hasListeners() {
        return HANDLERS.getRegisteredListeners().length > 0;
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
