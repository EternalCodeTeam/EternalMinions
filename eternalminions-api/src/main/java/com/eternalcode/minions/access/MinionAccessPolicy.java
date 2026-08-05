package com.eternalcode.minions.access;

import com.eternalcode.minions.minion.MinionDetails;
import org.bukkit.entity.Player;

@FunctionalInterface
public interface MinionAccessPolicy {

    /**
     * Checks whether this policy grants an additional player access to a minion.
     * The minion owner is always allowed before registered policies are evaluated.
     */
    boolean canAccess(
            Player player,
            MinionDetails minion,
            MinionAccessAction action
    );
}
