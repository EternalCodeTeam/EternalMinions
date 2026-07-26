package com.eternalcode.minions.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.eternalcode.minions.minion.MinionProgress;
import com.eternalcode.minions.minion.upgrade.MinionUpgrades;
import com.eternalcode.minions.minion.upgrade.CoreUpgradeKinds;
import org.junit.jupiter.api.Test;

class AbstractMinionConfigTest {

    @Test
    void resolvesCommonIntervalsAndCapacityByTheirDomainMeaning() {
        TestConfig config = new TestConfig();
        MinionUpgrades upgraded = MinionUpgrades.none()
            .withTier(CoreUpgradeKinds.SPEED, 1)
            .withTier(CoreUpgradeKinds.CAPACITY, 1);

        assertThat(config.workInterval(MinionUpgrades.none())).isEqualTo(40L);
        assertThat(config.workInterval(upgraded)).isEqualTo(30L);
        assertThat(config.storageCapacity(MinionUpgrades.none())).isEqualTo(9);
        assertThat(config.storageCapacity(upgraded)).isEqualTo(18);
    }

    @Test
    void advancesProgressUsingConfigThresholds() {
        TestConfig config = new TestConfig();
        config.levelThresholds = java.util.List.of(1L, 3L);

        MinionProgress levelTwo = MinionProgress.start().advanced(config);
        MinionProgress levelThree = levelTwo.advanced(config).advanced(config);

        assertThat(levelTwo).isEqualTo(new MinionProgress(2, 1L));
        assertThat(levelThree).isEqualTo(new MinionProgress(3, 3L));
        assertThat(config.maxLevel()).isEqualTo(3);
        assertThat(config.progressToReach(3)).isEqualTo(3L);
    }

    private static final class TestConfig extends AbstractMinionConfig {
    }
}
