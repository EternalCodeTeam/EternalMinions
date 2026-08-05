package com.eternalcode.minions.behavior;

import java.util.Collection;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

/** Read-only catalog of behaviors currently enabled by EternalMinions configuration. */
public interface MinionBehaviorService {

    /** Returns the behavior used when no explicit type is supplied. */
    @NotNull MinionBehaviorDetails defaultBehavior();

    /** Finds an enabled behavior by its case-insensitive identifier. */
    @NotNull Optional<MinionBehaviorDetails> findById(@NotNull String behaviorId);

    /** Returns an immutable snapshot of all enabled behaviors in display order. */
    @NotNull Collection<MinionBehaviorDetails> findAll();
}
