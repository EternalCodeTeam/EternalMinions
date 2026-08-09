package com.eternalcode.minions.bridge.shop.impl.vault;

import com.cryptomorin.xseries.XMaterial;
import com.eternalcode.minions.bridge.vault.VaultEconomyHook;
import com.eternalcode.minions.shop.MinionShopProvider;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.Server;

public final class VaultShopIntegration implements MinionShopProvider {

    private final Server server;
    private final VaultEconomyHook economy;
    private final Map<Material, Double> prices;

    public static VaultShopIntegration create(Server server, VaultEconomyHook economy, Map<XMaterial, Double> sellPrices) {
        return new VaultShopIntegration(server, economy, convert(sellPrices));
    }

    public VaultShopIntegration(Server server, VaultEconomyHook economy, Map<Material, Double> prices) {
        this.server = server;
        this.economy = economy;
        this.prices = Map.copyOf(prices);
    }

    private static Map<Material, Double> convert(Map<XMaterial, Double> sellPrices) {
        Map<Material, Double> converted = new EnumMap<>(Material.class);
        for (Map.Entry<XMaterial, Double> entry : sellPrices.entrySet()) {
            Material material = entry.getKey().get();
            if (material != null) {
                converted.put(material, entry.getValue());
            }
        }
        return converted;
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
