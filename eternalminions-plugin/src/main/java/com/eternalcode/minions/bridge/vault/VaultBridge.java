package com.eternalcode.minions.bridge.vault;

import com.eternalcode.minions.bridge.BridgeManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.bukkit.plugin.java.JavaPlugin;

public final class VaultBridge {

    private VaultBridge() {
    }

    /**
     * Discovers the Vault economy hook, if the Vault plugin is enabled. Any feature that needs
     * to move money (shop payouts, kit costs, teleport fees, ...) can call this independently -
     * it has nothing to do with any one specific consumer.
     */
    public static Optional<VaultEconomyHook> discover(BridgeManager bridgeManager, JavaPlugin plugin) {
        List<VaultEconomyHook> discovered = new ArrayList<>(1);

        bridgeManager.initialize(
                plugin.getServer().getPluginManager(),
                "Vault",
                () -> discovered.add(new VaultEconomyHook(plugin.getServer()))
        );

        return discovered.isEmpty() ? Optional.empty() : Optional.of(discovered.get(0));
    }
}
