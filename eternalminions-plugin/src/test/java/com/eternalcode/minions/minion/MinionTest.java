package com.eternalcode.minions.minion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class MinionTest {

    @Test
    void exposesImmutableApiDetails() {
        Minion minion = minion().withActive(false);

        assertThat(minion.details().active()).isFalse();
        assertThat(minion.details().level()).isEqualTo(1);
    }

    private static Minion minion() {
        return new Minion(
            new MinionId(7),
            UUID.randomUUID(),
            "miner",
            new MinionPosition("minecraft:world", 1, 70, 2),
            true,
            MinionProgress.start(),
            MinionEquipment.empty(),
            new MinionStorage(9),
            MinionUpgrades.none(),
            null,
            MinionSettings.defaults()
        );
    }
}
