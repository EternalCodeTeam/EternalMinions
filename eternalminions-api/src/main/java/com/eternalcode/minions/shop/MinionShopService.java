package com.eternalcode.minions.shop;

import org.bukkit.plugin.Plugin;

public interface MinionShopService {

    /**
     * Registers a shop provider owned by a plugin, so the Seller minion sells through it.
     * Only one externally registered provider may be active at a time: registering while a
     * different plugin's registration is still active throws IllegalStateException.
     * Registrations are automatically removed when the owning plugin is disabled.
     */
    MinionShopRegistration registerProvider(
            Plugin plugin,
            MinionShopProvider provider
    );
}
