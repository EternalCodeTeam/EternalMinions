package com.eternalcode.minions.database;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

class DatabaseManagerTest {

    // Regression test: a real Bukkit plugin's getDataFolder() is relative to the server's working
    // directory (e.g. "plugins/EternalMinions"), unlike JUnit's @TempDir which is always absolute.
    // H2 rejects a bare relative path in the JDBC URL, so DatabaseManager must resolve it first.
    @Test
    void connectsWithRelativeDataFolder() throws IOException {
        Path relativeFolder = Path.of("build", "tmp", "database-manager-relative-path-test");
        Files.createDirectories(relativeFolder);
        File dataFolder = new File(relativeFolder.toString());

        DatabaseConfig config = new DatabaseConfig();
        config.poolSize = 2;
        DatabaseManager database = new DatabaseManager(Logger.getLogger("DatabaseManagerTest"), dataFolder, config);

        try {
            assertThatCode(database::connect).doesNotThrowAnyException();
        }
        finally {
            database.close();
            deleteRecursively(relativeFolder);
        }
    }

    private static void deleteRecursively(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                }
                catch (IOException ignored) {
                }
            });
        }
    }
}
