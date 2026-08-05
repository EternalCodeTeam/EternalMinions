package com.eternalcode.minions;

import com.eternalcode.minions.access.MinionAccessService;
import com.eternalcode.minions.behavior.MinionBehaviorService;
import com.eternalcode.minions.item.MinionItemService;
import com.eternalcode.minions.minion.MinionManagementService;
import com.eternalcode.minions.minion.MinionService;
import com.eternalcode.minions.shop.MinionShopService;
import com.eternalcode.minions.status.MinionStatusService;
import org.jetbrains.annotations.NotNull;

/**
 * Stable entry point for integrations with EternalMinions.
 *
 * <p>Obtain the current instance through {@link EternalMinionsProvider#provide()} after the
 * EternalMinions plugin has been enabled. Returned services remain valid until the plugin is
 * disabled. Methods touching Bukkit objects or live minion state must be called from the server
 * thread unless their contract explicitly says otherwise.</p>
 */
public interface EternalMinionsApi {

    /** Returns the read-only query service for live minions. */
    @NotNull MinionService minionService();

    /** Returns the service for creating and changing live minions. */
    @NotNull MinionManagementService minionManagementService();

    /** Returns the catalog of currently enabled minion behaviors. */
    @NotNull MinionBehaviorService minionBehaviorService();

    /** Returns the service for recognizing and creating portable minion items. */
    @NotNull MinionItemService minionItemService();

    /** Returns the service exposing current runtime statuses. */
    @NotNull MinionStatusService minionStatusService();

    /** Returns the extension point used to add access rules from other plugins. */
    @NotNull MinionAccessService minionAccessService();

    /** Returns the extension point used to connect an external selling provider. */
    @NotNull MinionShopService minionShopService();
}
