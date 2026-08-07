package com.eternalcode.minions.minion.upgrade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class MinionUpgradesTest {

    @Test
    void shouldReturnZeroTierForUpgradeThatWasNeverPurchased() {
        assertThat(MinionUpgrades.none().tier(DefaultUpgradeKinds.SPEED)).isZero();
    }

    @Test
    void shouldStoreOnlyKindsWithNonZeroTier() {
        MinionUpgrades upgrades = MinionUpgrades.none()
                .withTier(DefaultUpgradeKinds.SPEED, 2)
                .withTier(DefaultUpgradeKinds.RANGE, 1);

        assertThat(upgrades.entries())
                .containsEntry(DefaultUpgradeKinds.SPEED, 2)
                .containsEntry(DefaultUpgradeKinds.RANGE, 1)
                .hasSize(2);
    }

    @Test
    void shouldRemoveEntryWhenTierIsSetBackToZero() {
        MinionUpgrades upgrades = MinionUpgrades.none().withTier(DefaultUpgradeKinds.CAPACITY, 3);

        MinionUpgrades reset = upgrades.withTier(DefaultUpgradeKinds.CAPACITY, 0);

        assertThat(reset.tier(DefaultUpgradeKinds.CAPACITY)).isZero();
        assertThat(reset.entries()).isEmpty();
    }

    @Test
    void shouldNotMutateOriginalInstanceWhenDerivingANewTier() {
        MinionUpgrades original = MinionUpgrades.none();

        MinionUpgrades updated = original.withTier(DefaultUpgradeKinds.SPEED, 1);

        assertThat(original.tier(DefaultUpgradeKinds.SPEED)).isZero();
        assertThat(updated.tier(DefaultUpgradeKinds.SPEED)).isEqualTo(1);
    }

    @Test
    void shouldRejectNegativeTier() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> MinionUpgrades.none().withTier(DefaultUpgradeKinds.SPEED, -1));
    }
}
