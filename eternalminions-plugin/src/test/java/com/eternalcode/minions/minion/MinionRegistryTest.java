package com.eternalcode.minions.minion;

import static org.assertj.core.api.Assertions.assertThat;

import com.eternalcode.minions.minion.storage.MinionSettings;
import com.eternalcode.minions.minion.storage.MinionStorage;
import com.eternalcode.minions.minion.upgrade.MinionUpgrades;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MinionRegistryTest {

    @Test
    void replaceReturnsPreviousMinion() {
        MinionRegistry registry = new MinionRegistry();
        Minion original = createMinion();
        Minion updated = original.withProgress(new MinionProgress(2, 10));
        registry.register(original);

        Minion previous = registry.replace(updated);

        assertThat(previous).isSameAs(original);
        assertThat(registry.findMinion(original.id())).containsSame(updated);
    }

    @Test
    void findsMinionAtPosition() {
        MinionRegistry registry = new MinionRegistry();
        Minion minion = createMinion();
        registry.register(minion);

        assertThat(registry.findAt(minion.position())).containsSame(minion);
        assertThat(registry.findAt(new MinionPosition("world", 1, 2, 4))).isEmpty();
    }

    @Test
    void replaceUpdatesPositionIndex() {
        MinionRegistry registry = new MinionRegistry();
        Minion original = createMinion();
        MinionPosition newPosition = new MinionPosition("world", 20, 4, 30);
        Minion moved = createMinion(newPosition);
        registry.register(original);

        registry.replace(moved);

        assertThat(registry.findAt(original.position())).isEmpty();
        assertThat(registry.findAt(newPosition)).containsSame(moved);
    }

    private static Minion createMinion() {
        return createMinion(new MinionPosition("world", 1, 2, 3));
    }

    private static Minion createMinion(MinionPosition position) {
        return new Minion(
            new MinionId(1),
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            "MINER",
            position,
            MinionProgress.start(),
            MinionEquipment.empty(),
            new MinionStorage(9),
            MinionUpgrades.none(),
            null,
            MinionSettings.defaults()
        );
    }
}
