package com.eternalcode.minions.minion;

import java.util.Optional;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Administrative mutation API for live minions.
 *
 * <p>These operations intentionally do not perform player permission or ownership checks. An
 * integration must authorize its caller before invoking them. Every method must run on the server
 * thread because it may update rendering, persistence and Bukkit inventory state.</p>
 */
public interface MinionManagementService {

    /**
     * Creates, persists and renders a fresh minion.
     *
     * @throws MinionOperationCancelledException when a listener cancels the pre-create event
     */
    @NotNull MinionSnapshot create(@NotNull MinionCreateRequest request);

    /** Removes a minion from runtime and persistence without creating a pickup item. Returns false
     * when the minion does not exist or a listener cancels the pre-remove event. */
    boolean remove(@NotNull MinionId minionId);

    /** Changes the minion direction and refreshes its renderer. */
    @NotNull Optional<MinionSnapshot> setDirection(
        @NotNull MinionId minionId,
        @NotNull MinionDirection direction
    );

    /** Links a chest position, or unlinks the current chest when {@code null}. */
    @NotNull Optional<MinionSnapshot> setChestPosition(
        @NotNull MinionId minionId,
        @Nullable MinionPosition position
    );

    /** Replaces one internal storage slot. Passing {@code null} clears it. */
    @NotNull Optional<MinionSnapshot> setStorageItem(
        @NotNull MinionId minionId,
        int slot,
        @Nullable ItemStack item
    );

    /** Replaces the equipped work tool. Passing {@code null} clears it. */
    @NotNull Optional<MinionSnapshot> setTool(
        @NotNull MinionId minionId,
        @Nullable ItemStack tool
    );

    /** Sets one upgrade tier after validating the behavior maximum and storage implications. */
    @NotNull Optional<MinionSnapshot> setUpgradeTier(
        @NotNull MinionId minionId,
        @NotNull String upgradeId,
        int tier
    );

    /** Sets progression values directly for administrative integrations. */
    @NotNull Optional<MinionSnapshot> setProgress(
        @NotNull MinionId minionId,
        int level,
        long progress
    );
}
