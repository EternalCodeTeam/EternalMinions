package com.eternalcode.minions.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.eternalcode.minions.minion.upgrade.DefaultUpgradeKinds;
import com.eternalcode.minions.minion.upgrade.MinionUpgrades;
import com.eternalcode.minions.minion.upgrade.UpgradeKind;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AbstractMinionConfigTest {

    @Test
    void shouldReturnFallbackValueWhenUpgradeWasNeverPurchased() {
        AbstractMinionConfig config = configWithSpeedTiers(30, 20);

        int value = config.upgradeTierValue(MinionUpgrades.none(), DefaultUpgradeKinds.SPEED, 40);

        assertThat(value).isEqualTo(40);
    }

    @Test
    void shouldReturnConfiguredTierValueForAPurchasedTier() {
        AbstractMinionConfig config = configWithSpeedTiers(30, 20);
        MinionUpgrades upgrades = MinionUpgrades.none().withTier(DefaultUpgradeKinds.SPEED, 1);

        int value = config.upgradeTierValue(upgrades, DefaultUpgradeKinds.SPEED, 40);

        assertThat(value).isEqualTo(30);
    }

    @Test
    void shouldClampToTheHighestConfiguredTierWhenPurchasedTierExceedsCurrentConfig() {
        // Simulates an admin removing higher tiers from config.yml after players already
        // purchased them: the persisted tier (e.g. 5) now exceeds what's configured (2 tiers).
        AbstractMinionConfig config = configWithSpeedTiers(30, 20);
        MinionUpgrades upgrades = MinionUpgrades.none().withTier(DefaultUpgradeKinds.SPEED, 5);

        int value = config.upgradeTierValue(upgrades, DefaultUpgradeKinds.SPEED, 40);

        assertThat(value)
                .as("a tier beyond what's configured must clamp to the highest available tier, not crash")
                .isEqualTo(20);
    }

    @Test
    void shouldNeverLetAnUpgradeLowerTheValueBelowTheConfiguredBaseline() {
        AbstractMinionConfig config = configWithRangeTiers(2);
        MinionUpgrades noUpgrade = MinionUpgrades.none();

        int value = config.upgradeTierValueOrHigher(noUpgrade, DefaultUpgradeKinds.RANGE, 4);

        assertThat(value).isEqualTo(4);
    }

    @Test
    void shouldRejectPurchasingATierThatDoesNotExist() {
        AbstractMinionConfig config = configWithSpeedTiers(30, 20);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> config.upgradeTier(DefaultUpgradeKinds.SPEED, 3));
    }

    @Test
    void shouldReportZeroMaxUpgradeTierForAnUnconfiguredKind() {
        AbstractMinionConfig config = configWithSpeedTiers(30, 20);

        assertThat(config.maxUpgradeTier(DefaultUpgradeKinds.CAPACITY)).isZero();
    }

    @Test
    void shouldComputeMaxLevelFromLevelThresholdCount() {
        AbstractMinionConfig config = plainConfig();
        config.levelThresholds = List.of(100L, 200L, 300L);

        assertThat(config.maxLevel()).isEqualTo(4);
    }

    @Test
    void shouldRejectQueryingProgressRequiredForAnOutOfRangeLevel() {
        AbstractMinionConfig config = plainConfig();
        config.levelThresholds = List.of(100L);

        assertThatIllegalArgumentException().isThrownBy(() -> config.progressToReach(1));
        assertThatIllegalArgumentException().isThrownBy(() -> config.progressToReach(3));
    }

    private static AbstractMinionConfig configWithSpeedTiers(int... values) {
        AbstractMinionConfig config = plainConfig();
        config.upgrades = tierMap(DefaultUpgradeKinds.SPEED, values);
        return config;
    }

    private static AbstractMinionConfig configWithRangeTiers(int... values) {
        AbstractMinionConfig config = plainConfig();
        config.upgrades = tierMap(DefaultUpgradeKinds.RANGE, values);
        return config;
    }

    private static Map<UpgradeKind, List<MinionUpgradeTierConfig>> tierMap(UpgradeKind kind, int... values) {
        List<MinionUpgradeTierConfig> tiers = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            MinionUpgradeTierConfig tier = new MinionUpgradeTierConfig(i + 2, values[i], new BigDecimal("10.00"));
            tiers.add(tier);
        }
        Map<UpgradeKind, List<MinionUpgradeTierConfig>> upgrades = new LinkedHashMap<>();
        upgrades.put(kind, tiers);
        return upgrades;
    }

    private static AbstractMinionConfig plainConfig() {
        return new AbstractMinionConfig() {
            @Override
            public Path resolve(Path dataDirectory) {
                return dataDirectory;
            }
        };
    }
}
