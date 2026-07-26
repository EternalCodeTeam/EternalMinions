package com.eternalcode.minions.access;

import com.eternalcode.minions.minion.MinionDetails;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public interface MinionAccessService {

    /**
     * Checks owner access and then policies registered by integration plugins.
     */
    boolean canAccess(
            @NotNull Player player,
            @NotNull MinionDetails minion,
            @NotNull MinionAccessAction action
    );

    /**
     * Registers an additional access policy owned by a plugin.
     * Registrations are automatically removed when the owning plugin is disabled.
     */
    @NotNull MinionAccessRegistration registerPolicy(
            @NotNull Plugin plugin,
            @NotNull MinionAccessPolicy policy
    );
}
