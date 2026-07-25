package com.eternalcode.minions.minion.seller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class NoopShopIntegrationTest {

    @Test
    void reportsUnavailableShopWithoutPrices() {
        ShopIntegration shop = new NoopShopIntegration();

        assertThat(shop.available()).isFalse();
        assertThat(shop.priceOf(Material.DIAMOND)).isZero();
    }

    @Test
    void ignoresPayoutWhenShopIsNotLinked() {
        ShopIntegration shop = new NoopShopIntegration();

        shop.payout(UUID.randomUUID(), 10.0D);

        assertThat(shop.available()).isFalse();
    }
}
