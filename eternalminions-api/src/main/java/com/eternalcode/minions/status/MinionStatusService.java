package com.eternalcode.minions.status;

import com.eternalcode.minions.minion.MinionId;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

/** Runtime status API used by dashboards, placeholders and administrative tooling. */
public interface MinionStatusService {

    /** Returns the current status key for a live minion. */
    @NotNull Optional<String> findStatus(@NotNull MinionId minionId);

    /** Overrides the current status key. Keys use uppercase letters, digits and underscores. */
    boolean setStatus(@NotNull MinionId minionId, @NotNull String statusKey);

    /** Clears an override and returns the minion to the default idle status. */
    boolean clearStatus(@NotNull MinionId minionId);
}
