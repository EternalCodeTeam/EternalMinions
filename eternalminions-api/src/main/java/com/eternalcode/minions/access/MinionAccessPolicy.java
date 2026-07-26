package com.eternalcode.minions.access;

import com.eternalcode.minions.minion.MinionDetails;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface MinionAccessPolicy {

    /**
     * Checks whether this policy grants an additional player access to a minion.
     * The minion owner is always allowed before registered policies are evaluated.
     */
    boolean canAccess(
            @NotNull Player player,
            @NotNull MinionDetails minion,
            @NotNull MinionAccessAction action
    );
}
