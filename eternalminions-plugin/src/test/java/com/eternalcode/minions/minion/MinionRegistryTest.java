package com.eternalcode.minions.minion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.UUID;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import org.junit.jupiter.api.Test;

class MinionRegistryTest {

    @Test
    void registersAndFindsMinion() {
        MinionRegistry registry = new MinionRegistry();
        Minion minion = minion(1L, UUID.randomUUID());

        registry.register(minion);

        assertThat(registry.findById(minion.id())).contains(minion.details());
        assertThat(registry.findMinion(minion.id())).contains(minion);
    }

    @Test
    void rejectsDuplicateIdentifier() {
        MinionRegistry registry = new MinionRegistry();
        Minion minion = minion(1L, UUID.randomUUID());
        registry.register(minion);

        assertThatIllegalArgumentException()
            .isThrownBy(() -> registry.register(minion))
            .withMessage("Minion 1 is already registered");
    }

    @Test
    void findsMinionsByOwnerWithoutMaintainingSecondIndex() {
        MinionRegistry registry = new MinionRegistry();
        UUID ownerId = UUID.randomUUID();
        Minion first = minion(1L, ownerId);
        Minion second = minion(2L, ownerId);
        registry.register(first);
        registry.register(second);

        assertThat(registry.findByOwner(ownerId)).containsExactlyInAnyOrder(first.details(), second.details());
    }

    @Test
    void removesMinionFromPrimaryIndex() {
        MinionRegistry registry = new MinionRegistry();
        Minion minion = minion(1L, UUID.randomUUID());
        registry.register(minion);

        assertThat(registry.remove(minion.id())).contains(minion);
        assertThat(registry.findById(minion.id())).isEmpty();
    }

    @Test
    void indexesMinionsByWorldChunk() {
        MinionRegistry registry = new MinionRegistry();
        Minion minion = minion(1L, UUID.randomUUID());
        registry.register(minion);
        LongArrayList indexedIds = new LongArrayList();

        registry.forEachMinionIdInChunk("minecraft:world", 0, 1, indexedIds::add);
        assertThat(indexedIds.toLongArray()).containsExactly(1L);

        registry.remove(minion.id());
        indexedIds.clear();
        registry.forEachMinionIdInChunk("minecraft:world", 0, 1, indexedIds::add);
        assertThat(indexedIds.toLongArray()).isEmpty();
    }

    private static Minion minion(long id, UUID ownerId) {
        return new Minion(
            new MinionId(id),
            ownerId,
            "miner",
            new MinionPosition("minecraft:world", 10, 64, 20),
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
