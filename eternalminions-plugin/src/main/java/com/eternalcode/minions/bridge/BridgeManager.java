package com.eternalcode.minions.bridge;

import org.bukkit.plugin.PluginManager;

public final class BridgeManager {

    public void initialize(PluginManager pluginManager, String pluginName, BridgeInitializer initializer) {
        if (!pluginManager.isPluginEnabled(pluginName)) {
            return;
        }

        initializer.initialize();
    }
}
