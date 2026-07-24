package com.eternalcode.minions.database;

import java.nio.file.Path;
import java.util.logging.Logger;

final class DatabaseTestSupport {

    private DatabaseTestSupport() {
    }

    static DatabaseManager create(Path temporaryDirectory) {
        DatabaseConfig config = new DatabaseConfig();
        config.poolSize = 2;
        return new DatabaseManager(
            Logger.getLogger("MinionRepositoryTest"),
            temporaryDirectory.toFile(),
            config
        );
    }
}
