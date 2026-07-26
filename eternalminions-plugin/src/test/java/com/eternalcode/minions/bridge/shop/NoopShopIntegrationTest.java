package com.eternalcode.minions.bridge.shop;

import static org.assertj.core.api.Assertions.assertThat;

import com.eternalcode.minions.shop.MinionShopProvider;
import java.util.UUID;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class NoopShopIntegrationTest {

    @Test
    void reportsUnavailableShopWithoutPrices() {
        MinionShopProvider shop = new NoopShopIntegration();

        assertThat(shop.available()).isFalse();
        assertThat(shop.priceOf(Material.DIAMOND)).isZero();
    }

    @Test
    void ignoresPayoutWhenShopIsNotLinked() {
        MinionShopProvider shop = new NoopShopIntegration();

        shop.payout(UUID.randomUUID(), 10.0D);

        assertThat(shop.available()).isFalse();
    }
}
