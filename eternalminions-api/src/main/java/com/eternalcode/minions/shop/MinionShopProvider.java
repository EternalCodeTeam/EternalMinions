package com.eternalcode.minions.shop;

import java.util.UUID;
import org.bukkit.Material;

public interface MinionShopProvider {

    boolean available();

    double priceOf(Material material);

    void payout(UUID ownerId, double amount);
}
