package com.eternalcode.minions.database;

import com.eternalcode.commons.scheduler.Scheduler;
import com.j256.ormlite.support.DatabaseConnection;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

final class MinionSchema {

    private static final List<String> CHILD_TABLES = List.of(
            "eternal_minion_state",
            "eternal_minion_settings",
            "eternal_minion_equipment",
            "eternal_minion_storage",
            "eternal_minion_upgrades",
            "eternal_minion_chest_links"
    );

    private final DatabaseManager databaseManager;
    private final Scheduler scheduler;

    MinionSchema(DatabaseManager databaseManager, Scheduler scheduler) {
        this.databaseManager = databaseManager;
        this.scheduler = scheduler;
    }

    private static boolean hasIndex(Connection connection, String table, String index) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        String tableName = metadata.storesUpperCaseIdentifiers()
            ? table.toUpperCase(Locale.ROOT)
            : table;
        try (ResultSet indexes = metadata.getIndexInfo(connection.getCatalog(), null, tableName, false, false)) {
            while (indexes.next()) {
                String existingIndex = indexes.getString("INDEX_NAME");
                if (existingIndex != null && existingIndex.equalsIgnoreCase(index)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String binaryType(DatabaseMetaData metadata) throws Exception {
        return metadata.getDatabaseProductName().contains("PostgreSQL") ? "BYTEA" : "BLOB";
    }

    CompletableFuture<Void> initialize() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        this.scheduler.runAsync(() -> {
            try {
                this.databaseManager.connect();
                this.createTables();
                future.complete(null);
            }
            catch (Exception exception) {
                future.completeExceptionally(new DatabaseException("Failed to create minion schema", exception));
            }
        });
        return future;
    }

    private void createTables() throws Exception {
        DatabaseConnection databaseConnection =
                this.databaseManager.connectionSource().getReadWriteConnection("minion-schema");
        try {
            Connection connection = databaseConnection.getUnderlyingConnection();
            String binaryType = binaryType(connection.getMetaData());
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS eternal_minions (
                            id BIGINT NOT NULL PRIMARY KEY,
                            owner_id VARCHAR(36) NOT NULL,
                            behavior_id VARCHAR(128) NOT NULL,
                            world_key VARCHAR(255) NOT NULL,
                            block_x INTEGER NOT NULL,
                            block_y INTEGER NOT NULL,
                            block_z INTEGER NOT NULL,
                            created_at BIGINT NOT NULL
                        )
                        """);
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS eternal_minion_state (
                            minion_id BIGINT NOT NULL PRIMARY KEY,
                            active BOOLEAN NOT NULL,
                            level INTEGER NOT NULL,
                            progress BIGINT NOT NULL,
                            updated_at BIGINT NOT NULL,
                            FOREIGN KEY (minion_id) REFERENCES eternal_minions(id) ON DELETE CASCADE
                        )
                        """);
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS eternal_minion_settings (
                            minion_id BIGINT NOT NULL PRIMARY KEY,
                            direction VARCHAR(32) NOT NULL,
                            mining_mode VARCHAR(32) NOT NULL,
                            FOREIGN KEY (minion_id) REFERENCES eternal_minions(id) ON DELETE CASCADE
                        )
                        """);
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS eternal_minion_equipment (
                            minion_id BIGINT NOT NULL,
                            slot VARCHAR(64) NOT NULL,
                            serialized_item %s NOT NULL,
                            PRIMARY KEY (minion_id, slot),
                            FOREIGN KEY (minion_id) REFERENCES eternal_minions(id) ON DELETE CASCADE
                        )
                        """.formatted(binaryType));
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS eternal_minion_storage (
                            minion_id BIGINT NOT NULL,
                            slot INTEGER NOT NULL,
                            serialized_item %s NOT NULL,
                            PRIMARY KEY (minion_id, slot),
                            FOREIGN KEY (minion_id) REFERENCES eternal_minions(id) ON DELETE CASCADE
                        )
                        """.formatted(binaryType));
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS eternal_minion_upgrades (
                            minion_id BIGINT NOT NULL,
                            upgrade_type VARCHAR(64) NOT NULL,
                            tier INTEGER NOT NULL,
                            PRIMARY KEY (minion_id, upgrade_type),
                            FOREIGN KEY (minion_id) REFERENCES eternal_minions(id) ON DELETE CASCADE
                        )
                        """);
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS eternal_minion_chest_links (
                            minion_id BIGINT NOT NULL PRIMARY KEY,
                            world_key VARCHAR(255) NOT NULL,
                            block_x INTEGER NOT NULL,
                            block_y INTEGER NOT NULL,
                            block_z INTEGER NOT NULL,
                            FOREIGN KEY (minion_id) REFERENCES eternal_minions(id) ON DELETE CASCADE
                        )
                        """);
            }
            this.createIndex(connection, "eternal_minions", "eternal_minions_owner_idx", "owner_id");
            this.createIndex(
                    connection,
                    "eternal_minions",
                    "eternal_minions_position_idx",
                    "world_key, block_x, block_z"
            );
        } finally {
            this.databaseManager.connectionSource().releaseConnection(databaseConnection);
        }
    }

    private void createIndex(Connection connection, String table, String index, String columns) throws Exception {
        if (hasIndex(connection, table, index)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE INDEX " + index + " ON " + table + " (" + columns + ")");
        }
    }
}
