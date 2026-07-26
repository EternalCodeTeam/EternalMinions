package com.eternalcode.minions.bridge;

import java.util.logging.Logger;
import org.bukkit.plugin.PluginManager;

public final class BridgeManager {

    private final Logger logger;

    public BridgeManager(Logger logger) {
        this.logger = logger;
    }

    public void initialize(PluginManager pluginManager, String pluginName, BridgeInitializer initializer) {
        if (!pluginManager.isPluginEnabled(pluginName)) {
            this.logger.warning(pluginName + " not found; skipping " + pluginName + " bridge.");
            return;
        }

        initializer.initialize();
        this.logger.info("Initialized " + pluginName + " bridge.");
    }
}
