package com.eternalcode.minions.shop;

import java.util.UUID;
import org.bukkit.Material;

public interface MinionShopProvider {

    boolean available();

    double priceOf(Material material);

    void payout(UUID ownerId, double amount);

    /**
     * Attempts to pay the owner and reports whether the provider completed the payout.
     *
     * @param ownerId owner receiving the money
     * @param amount amount to pay
     * @return true when the payout completed
     */
    default boolean tryPayout(UUID ownerId, double amount) {
        this.payout(ownerId, amount);
        return true;
    }
}
