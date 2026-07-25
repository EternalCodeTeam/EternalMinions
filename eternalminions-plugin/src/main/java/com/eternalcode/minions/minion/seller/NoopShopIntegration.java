package com.eternalcode.minions.minion.seller;

import java.util.UUID;
import org.bukkit.Material;

public final class NoopShopIntegration implements ShopIntegration {

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public double priceOf(Material material) {
        return 0.0D;
    }

    @Override
    public void payout(UUID ownerId, double amount) {
    }
}
