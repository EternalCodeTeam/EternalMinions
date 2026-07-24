package com.eternalcode.minions.database;

import static org.assertj.core.api.Assertions.assertThat;

import com.j256.ormlite.support.DatabaseConnection;
import java.nio.file.Path;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MinionSchemaTest {

    @Test
    void createsCompositeKeysAndCascadingForeignKeys(@TempDir Path temporaryDirectory) throws Exception {
        DatabaseManager database = DatabaseTestSupport.create(temporaryDirectory);

        try {
            new MinionSchema(database, new ImmediateScheduler()).initialize().join();

            assertThat(primaryKeys(database, "ETERNAL_MINION_EQUIPMENT"))
                .containsExactlyInAnyOrder("MINION_ID", "SLOT");
            assertThat(primaryKeys(database, "ETERNAL_MINION_STORAGE"))
                .containsExactlyInAnyOrder("MINION_ID", "SLOT");
            assertThat(primaryKeys(database, "ETERNAL_MINION_UPGRADES"))
                .containsExactlyInAnyOrder("MINION_ID", "UPGRADE_TYPE");
            assertThat(cascadeDeleteTables(database))
                .contains(
                    "ETERNAL_MINION_STATE",
                    "ETERNAL_MINION_SETTINGS",
                    "ETERNAL_MINION_EQUIPMENT",
                    "ETERNAL_MINION_STORAGE",
                    "ETERNAL_MINION_UPGRADES",
                    "ETERNAL_MINION_CHEST_LINKS"
                );
        }
        finally {
            database.close();
        }
    }

    private static List<String> primaryKeys(DatabaseManager database, String tableName) throws Exception {
        return inspect(database, metadata -> metadata.getPrimaryKeys(null, null, tableName), "COLUMN_NAME");
    }

    private static List<String> cascadeDeleteTables(DatabaseManager database) throws Exception {
        DatabaseConnection connection = database.connectionSource().getReadOnlyConnection("schema-test");
        try {
            DatabaseMetaData metadata = connection.getUnderlyingConnection().getMetaData();
            List<String> tables = new ArrayList<>();
            for (String tableName : List.of(
                "ETERNAL_MINION_STATE",
                "ETERNAL_MINION_SETTINGS",
                "ETERNAL_MINION_EQUIPMENT",
                "ETERNAL_MINION_STORAGE",
                "ETERNAL_MINION_UPGRADES",
                "ETERNAL_MINION_CHEST_LINKS"
            )) {
                try (ResultSet keys = metadata.getImportedKeys(null, null, tableName)) {
                    while (keys.next()) {
                        if (keys.getShort("DELETE_RULE") == DatabaseMetaData.importedKeyCascade) {
                            tables.add(tableName);
                        }
                    }
                }
            }
            return tables;
        }
        finally {
            database.connectionSource().releaseConnection(connection);
        }
    }

    private static List<String> inspect(
        DatabaseManager database,
        MetadataQuery query,
        String column
    ) throws Exception {
        DatabaseConnection connection = database.connectionSource().getReadOnlyConnection("schema-test");
        try {
            List<String> values = new ArrayList<>();
            try (ResultSet rows = query.execute(connection.getUnderlyingConnection().getMetaData())) {
                while (rows.next()) {
                    values.add(rows.getString(column));
                }
            }
            return values;
        }
        finally {
            database.connectionSource().releaseConnection(connection);
        }
    }

    @FunctionalInterface
    private interface MetadataQuery {

        ResultSet execute(DatabaseMetaData metadata) throws Exception;
    }
}
