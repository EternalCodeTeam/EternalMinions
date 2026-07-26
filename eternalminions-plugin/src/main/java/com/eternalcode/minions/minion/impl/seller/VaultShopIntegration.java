package com.eternalcode.minions.minion.impl.seller;

import com.eternalcode.minions.integration.VaultEconomyHook;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.Server;

public final class VaultShopIntegration implements ShopIntegration {

    private final Server server;
    private final VaultEconomyHook economy;
    private final Map<Material, Double> prices;

    public VaultShopIntegration(Server server, VaultEconomyHook economy, Map<Material, Double> prices) {
        this.server = server;
        this.economy = economy;
        this.prices = Map.copyOf(prices);
    }

    @Override
    public boolean available() {
        return this.economy.available();
    }

    @Override
    public double priceOf(Material material) {
        Double price = this.prices.get(material);
        return price == null ? 0.0D : price;
    }

    @Override
    public void payout(UUID ownerId, double amount) {
        this.economy.deposit(this.server.getOfflinePlayer(ownerId), amount);
    }
}
