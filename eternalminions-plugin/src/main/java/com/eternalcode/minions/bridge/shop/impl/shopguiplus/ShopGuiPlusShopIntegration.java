package com.eternalcode.minions.bridge.shop.impl.shopguiplus;

import com.eternalcode.minions.bridge.vault.VaultEconomyHook;
import com.eternalcode.minions.shop.MinionShopProvider;
import java.util.UUID;
import net.brcdev.shopgui.ShopGuiPlusApi;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class ShopGuiPlusShopIntegration implements MinionShopProvider {

    private final Server server;
    private final VaultEconomyHook economy;
    private volatile boolean ready;

    public static ShopGuiPlusShopIntegration create(JavaPlugin plugin, VaultEconomyHook economy) {
        ShopGuiPlusShopIntegration integration = new ShopGuiPlusShopIntegration(plugin.getServer(), economy);
        plugin.getServer().getPluginManager().registerEvents(new ShopGuiPlusHookListener(integration), plugin);
        return integration;
    }

    public ShopGuiPlusShopIntegration(Server server, VaultEconomyHook economy) {
        this.server = server;
        this.economy = economy;
    }

    @Override
    public boolean available() {
        return this.ready && this.economy.available();
    }

    @Override
    public double priceOf(Material material) {
        double price = ShopGuiPlusApi.getItemStackPriceSell(new ItemStack(material, 1));
        return price > 0.0D ? price : 0.0D;
    }

    @Override
    public void payout(UUID ownerId, double amount) {
        this.economy.deposit(this.server.getOfflinePlayer(ownerId), amount);
    }

    void markReady() {
        this.ready = true;
    }
}
