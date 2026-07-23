package com.eternalcode.minions.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
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
            new byte[0],
            List.of()
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
