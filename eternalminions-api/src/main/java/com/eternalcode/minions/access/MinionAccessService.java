package com.eternalcode.minions.access;

import com.eternalcode.minions.minion.MinionDetails;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public interface MinionAccessService {

    /**
     * Checks owner access and then policies registered by integration plugins.
     */
    boolean canAccess(
            Player player,
            MinionDetails minion,
            MinionAccessAction action
    );

    /**
     * Registers an additional access policy owned by a plugin.
     * Registrations are automatically removed when the owning plugin is disabled.
     */
    MinionAccessRegistration registerPolicy(
            Plugin plugin,
            MinionAccessPolicy policy
    );
}
