package com.eternalcode.minions.database;

import static org.assertj.core.api.Assertions.assertThat;

import com.eternalcode.minions.minion.MiningMode;
import com.eternalcode.minions.minion.MinionDirection;
import com.eternalcode.minions.minion.MinionSettings;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MinionRepositoryTest {

    @Test
    void storesLoadsAndDeletesNormalizedMinionData(@TempDir Path temporaryDirectory) throws Exception {
        DatabaseConfig config = new DatabaseConfig();
        config.maximumPoolSize = 2;
        MinionDatabase database = new MinionDatabase(config, temporaryDirectory.toFile());
        MinionRepository repository = new MinionRepository(database, Runnable::run);
        MinionData data = new MinionData(
            41L,
            UUID.randomUUID(),
            "miner",
            "minecraft:world",
            4,
            70,
            -8,
            true,
            3,
            5_500L,
            new byte[0],
            List.of(),
            Map.of("SPEED", 1, "CAPACITY", 2),
            new MinionData.ChestPositionData("minecraft:world", 6, 70, -9),
            new MinionSettings(MinionDirection.WEST, MiningMode.LINEAR)
        );

        try {
            repository.initialize().join();
            repository.save(List.of(data)).join();

            assertThat(repository.loadAll().join()).usingRecursiveComparison().isEqualTo(List.of(data));

            repository.delete(data.id()).join();
            assertThat(repository.loadAll().join()).isEmpty();
        }
        finally {
            database.close();
        }
    }
}
