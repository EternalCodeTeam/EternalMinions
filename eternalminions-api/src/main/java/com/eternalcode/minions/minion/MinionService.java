package com.eternalcode.minions.minion;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/** Read-only queries over minions currently loaded in the EternalMinions runtime. */
public interface MinionService {

    /** Returns the public identity and placement details for a live minion. */
    @NotNull Optional<MinionDetails> findById(@NotNull MinionId minionId);

    /** Returns an immutable snapshot of one live minion, including mutable gameplay state. */
    @NotNull Optional<MinionSnapshot> findSnapshotById(@NotNull MinionId minionId);

    /** Returns immutable identity snapshots of all currently loaded minions. */
    @NotNull Collection<MinionDetails> findAll();

    /** Returns immutable state snapshots of all currently loaded minions. */
    @NotNull Collection<MinionSnapshot> findAllSnapshots();

    /** Returns all currently loaded minions owned by the supplied player UUID. */
    @NotNull Collection<MinionDetails> findByOwner(@NotNull UUID ownerId);

    /** Returns the number of currently loaded minions owned by the supplied player UUID. */
    int countByOwner(@NotNull UUID ownerId);

    /** Finds a live minion occupying the exact block position. */
    @NotNull Optional<MinionDetails> findAt(@NotNull MinionPosition position);
}
