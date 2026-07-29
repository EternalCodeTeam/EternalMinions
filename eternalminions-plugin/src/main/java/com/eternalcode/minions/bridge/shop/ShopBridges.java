package com.eternalcode.minions.bridge.shop;

import com.cryptomorin.xseries.XMaterial;
import com.eternalcode.minions.bridge.BridgeManager;
import com.eternalcode.minions.bridge.shop.impl.shopguiplus.ShopGuiPlusShopIntegration;
import com.eternalcode.minions.bridge.shop.impl.vault.VaultShopIntegration;
import com.eternalcode.minions.bridge.vault.VaultEconomyHook;
import com.eternalcode.minions.shop.MinionShopProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class ShopBridges {

    private ShopBridges() {
    }

    public static List<MinionShopProvider> discover(
            BridgeManager bridgeManager,
            JavaPlugin plugin,
            Map<XMaterial, Double> sellPrices,
            Optional<VaultEconomyHook> economy
    ) {
        List<MinionShopProvider> hooks = new ArrayList<>();

        if (economy.isEmpty()) {
            return hooks;
        }

        PluginManager pluginManager = plugin.getServer().getPluginManager();

        bridgeManager.initialize(
                pluginManager,
                "ShopGUIPlus",
                () -> hooks.add(ShopGuiPlusShopIntegration.create(plugin, economy.get()))
        );

        hooks.add(VaultShopIntegration.create(plugin.getServer(), economy.get(), sellPrices));

        return hooks;
    }
}
