package com.eternalcode.minions.minion.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;
import org.junit.jupiter.api.Test;

class ToolDurabilityServiceTest {

    @Test
    void unbreakingZeroAlwaysDamages() {
        Random random = new Random(1L);

        for (int attempt = 0; attempt < 100; attempt++) {
            assertThat(ToolDurabilityService.shouldApplyDamage(0, random)).isTrue();
        }
    }

    @Test
    void higherUnbreakingLevelReducesDamageFrequency() {
        Random random = new Random(42L);
        int damagedAtLevelZero = 0;
        int damagedAtLevelThree = 0;

        for (int attempt = 0; attempt < 10_000; attempt++) {
            if (ToolDurabilityService.shouldApplyDamage(0, random)) {
                damagedAtLevelZero++;
            }
        }
        for (int attempt = 0; attempt < 10_000; attempt++) {
            if (ToolDurabilityService.shouldApplyDamage(3, random)) {
                damagedAtLevelThree++;
            }
        }

        assertThat(damagedAtLevelZero).isEqualTo(10_000);
        assertThat(damagedAtLevelThree).isBetween(2_000, 3_000);
    }
}
