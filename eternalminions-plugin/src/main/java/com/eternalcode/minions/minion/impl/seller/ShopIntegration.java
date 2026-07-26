package com.eternalcode.minions.minion.impl.seller;

import java.util.UUID;
import org.bukkit.Material;

public interface ShopIntegration {

    boolean available();

    double priceOf(Material material);

    void payout(UUID ownerId, double amount);
}
