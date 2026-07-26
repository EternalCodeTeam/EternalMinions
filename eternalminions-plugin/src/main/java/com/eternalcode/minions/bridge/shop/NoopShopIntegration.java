package com.eternalcode.minions.bridge.shop;

import com.eternalcode.minions.shop.MinionShopProvider;
import java.util.UUID;
import org.bukkit.Material;

public final class NoopShopIntegration implements MinionShopProvider {

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
