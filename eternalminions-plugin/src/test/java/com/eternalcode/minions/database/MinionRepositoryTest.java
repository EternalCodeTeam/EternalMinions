package com.eternalcode.minions.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eternalcode.minions.minion.MinionDirection;
import com.eternalcode.minions.minion.MinionId;
import com.eternalcode.minions.minion.MinionPosition;
import com.eternalcode.minions.minion.storage.MinionSettings;
import com.eternalcode.minions.minion.upgrade.CoreUpgradeKinds;
import com.eternalcode.minions.minion.MiningMode;
import com.j256.ormlite.support.DatabaseConnection;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.h2.api.Trigger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MinionRepositoryTest {

    @Test
    void createsAndLoadsCompleteMinion(@TempDir Path temporaryDirectory) {
        RepositoryContext context = context(temporaryDirectory);
        MinionData minion = minion(41L, UUID.randomUUID(), "minecraft:world", 4, -8);

        try {
            context.minions.create(minion).join();

            assertThat(context.minions.findById(new MinionId(minion.id())).join().orElseThrow())
                .usingRecursiveComparison()
                .isEqualTo(minion);
            assertThat(context.minions.loadAll().join())
                .usingRecursiveComparison()
                .isEqualTo(List.of(minion));
        }
        finally {
            context.database.close();
        }
    }

    @Test
    void updatesOnlyMinionState(@TempDir Path temporaryDirectory) {
        RepositoryContext context = context(temporaryDirectory);
        MinionData minion = minion(7L, UUID.randomUUID(), "minecraft:world", 1, 1);

        try {
            context.minions.create(minion).join();
            context.states.saveState(new MinionId(minion.id()), false, 4, 8_000L, 2_000L).join();

            MinionData loaded = context.minions.findById(new MinionId(minion.id())).join().orElseThrow();
            assertThat(loaded)
                .usingRecursiveComparison()
                .ignoringFields("active", "level", "progress")
                .isEqualTo(minion);
            assertThat(loaded.active()).isFalse();
            assertThat(loaded.level()).isEqualTo(4);
            assertThat(loaded.progress()).isEqualTo(8_000L);
        }
        finally {
            context.database.close();
        }
    }

    @Test
    void updatesOneStorageSlotWithoutChangingOtherSlots(@TempDir Path temporaryDirectory) {
        RepositoryContext context = context(temporaryDirectory);
        MinionData minion = minion(8L, UUID.randomUUID(), "minecraft:world", 2, 2);

        try {
            context.minions.create(minion).join();
            context.storage.saveSlot(new MinionId(minion.id()), 0, new byte[] {9, 9}).join();

            assertThat(context.minions.findById(new MinionId(minion.id())).join().orElseThrow().storageItems())
                .usingRecursiveComparison()
                .isEqualTo(List.of(
                    new StoredItemData(0, new byte[] {9, 9}),
                    new StoredItemData(3, new byte[] {6, 7})
                ));
        }
        finally {
            context.database.close();
        }
    }

    @Test
    void deletesOneStorageSlot(@TempDir Path temporaryDirectory) {
        RepositoryContext context = context(temporaryDirectory);
        MinionData minion = minion(9L, UUID.randomUUID(), "minecraft:world", 3, 3);

        try {
            context.minions.create(minion).join();
            context.storage.deleteSlot(new MinionId(minion.id()), 0).join();

            assertThat(context.minions.findById(new MinionId(minion.id())).join().orElseThrow().storageItems())
                .usingRecursiveComparison()
                .isEqualTo(List.of(new StoredItemData(3, new byte[] {6, 7})));
        }
        finally {
            context.database.close();
        }
    }

    @Test
    void updatesOneUpgrade(@TempDir Path temporaryDirectory) {
        RepositoryContext context = context(temporaryDirectory);
        MinionData minion = minion(10L, UUID.randomUUID(), "minecraft:world", 4, 4);

        try {
            context.minions.create(minion).join();
            context.upgrades.saveUpgrade(new MinionId(minion.id()), CoreUpgradeKinds.SPEED, 4).join();

            assertThat(context.minions.findById(new MinionId(minion.id())).join().orElseThrow().upgrades())
                .containsEntry("SPEED", 4)
                .containsEntry("CAPACITY", 2);
        }
        finally {
            context.database.close();
        }
    }

    @Test
    void linksAndUnlinksChest(@TempDir Path temporaryDirectory) {
        RepositoryContext context = context(temporaryDirectory);
        MinionData minion = minion(11L, UUID.randomUUID(), "minecraft:world", 5, 5);
        MinionId minionId = new MinionId(minion.id());

        try {
            context.minions.create(minion).join();
            context.chests.deleteLink(minionId).join();
            assertThat(context.minions.findById(minionId).join().orElseThrow().chestPosition()).isNull();

            context.chests.saveLink(minionId, new MinionPosition("minecraft:nether", 20, 70, 21)).join();
            assertThat(context.minions.findById(minionId).join().orElseThrow().chestPosition())
                .isEqualTo(new MinionData.ChestPositionData("minecraft:nether", 20, 70, 21));
        }
        finally {
            context.database.close();
        }
    }

    @Test
    void deletesDependentRowsThroughCascade(@TempDir Path temporaryDirectory) throws Exception {
        RepositoryContext context = context(temporaryDirectory);
        MinionData minion = minion(12L, UUID.randomUUID(), "minecraft:world", 6, 6);

        try {
            context.minions.create(minion).join();
            context.minions.deleteMinion(new MinionId(minion.id())).join();

            for (String table : List.of(
                "eternal_minion_state",
                "eternal_minion_settings",
                "eternal_minion_equipment",
                "eternal_minion_storage",
                "eternal_minion_upgrades",
                "eternal_minion_chest_links"
            )) {
                assertThat(countRows(context.database, table)).isZero();
            }
        }
        finally {
            context.database.close();
        }
    }

    @Test
    void rollsBackCreationWhenChildInsertFails(@TempDir Path temporaryDirectory) throws Exception {
        RepositoryContext context = context(temporaryDirectory);
        MinionData minion = minion(13L, UUID.randomUUID(), "minecraft:world", 7, 7);

        try {
            execute(context.database, """
                CREATE TRIGGER fail_storage
                BEFORE INSERT ON eternal_minion_storage
                FOR EACH ROW CALL 'com.eternalcode.minions.database.MinionRepositoryTest$FailingStorageTrigger'
                """);

            assertThatThrownBy(() -> context.minions.create(minion).join())
                .hasCauseInstanceOf(DatabaseException.class);
            assertThat(context.minions.findById(new MinionId(minion.id())).join()).isEmpty();
        }
        finally {
            context.database.close();
        }
    }

    @Test
    void loadsByOwnerAndChunkWithFixedBatchQueries(@TempDir Path temporaryDirectory) throws Exception {
        RepositoryContext context = context(temporaryDirectory);
        UUID ownerId = UUID.randomUUID();
        MinionData first = minion(20L, ownerId, "minecraft:world", 1, 1);
        MinionData second = minion(21L, ownerId, "minecraft:world", 15, 15);
        MinionData other = minion(22L, UUID.randomUUID(), "minecraft:world", 16, 16);

        try {
            context.minions.create(first).join();
            context.minions.create(second).join();
            context.minions.create(other).join();
            resetQueryStatistics(context.database);

            assertThat(context.minions.findByOwner(ownerId).join())
                .usingRecursiveComparison()
                .isEqualTo(List.of(first, second));
            assertThat(minionSelectCount(context.database)).isEqualTo(7L);

            resetQueryStatistics(context.database);
            assertThat(context.minions.findByChunk("minecraft:world", 0, 0).join())
                .usingRecursiveComparison()
                .isEqualTo(List.of(first, second));
            assertThat(minionSelectCount(context.database)).isEqualTo(7L);
        }
        finally {
            context.database.close();
        }
    }

    @Test
    void fallsBackFromUnknownSettingsAndLoadsAnyWellFormedUpgradeKind(@TempDir Path temporaryDirectory) throws Exception {
        RepositoryContext context = context(temporaryDirectory);
        MinionData minion = minion(23L, UUID.randomUUID(), "minecraft:world", 1, 1);

        try {
            context.minions.create(minion).join();
            execute(context.database, """
                UPDATE eternal_minion_settings
                SET direction = 'REMOVED_DIRECTION', mining_mode = 'REMOVED_MODE'
                WHERE minion_id = 23
                """);
            // Upgrade kinds are an open value type now (not a closed enum), so a kind no longer
            // declared by any profession still loads verbatim - only the profession's own
            // The registered behavior decides whether it means anything, not the persistence layer.
            execute(context.database, """
                INSERT INTO eternal_minion_upgrades (minion_id, upgrade_type, tier)
                VALUES (23, 'REMOVED_UPGRADE', 5)
                """);

            MinionData loaded = context.minions.findById(new MinionId(minion.id())).join().orElseThrow();
            assertThat(loaded.settings()).isEqualTo(MinionSettings.defaults());
            assertThat(loaded.upgrades()).containsEntry("REMOVED_UPGRADE", 5);
            assertThat(loaded.upgrades()).containsEntry("SPEED", 1);
        }
        finally {
            context.database.close();
        }
    }

    @Test
    void validatesRowsAndDefensivelyCopiesSerializedItems(@TempDir Path temporaryDirectory) {
        RepositoryContext context = context(temporaryDirectory);
        MinionData minion = minion(24L, UUID.randomUUID(), "minecraft:world", 1, 1);

        try {
            context.minions.create(minion).join();

            assertThatThrownBy(() -> context.storage.saveSlot(new MinionId(24L), -1, new byte[] {1}))
                .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> context.storage.saveSlot(new MinionId(24L), 1, new byte[0]))
                .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> context.states.saveState(new MinionId(24L), true, -1, 0L, 1L))
                .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() ->
                context.upgrades.saveUpgrade(new MinionId(24L), CoreUpgradeKinds.SPEED, -1))
                .isInstanceOf(IllegalArgumentException.class);

            MinionData loaded = context.minions.findById(new MinionId(24L)).join().orElseThrow();
            byte[] tool = loaded.serializedTool();
            tool[0] = 99;
            byte[] storedItem = loaded.storageItems().getFirst().serializedItem();
            storedItem[0] = 99;

            MinionData loadedAgain = context.minions.findById(new MinionId(24L)).join().orElseThrow();
            assertThat(loadedAgain.serializedTool()).containsExactly(1, 2, 3);
            assertThat(loadedAgain.storageItems().getFirst().serializedItem()).containsExactly(4, 5);
        }
        finally {
            context.database.close();
        }
    }

    private static RepositoryContext context(Path temporaryDirectory) {
        DatabaseManager database = DatabaseTestSupport.create(temporaryDirectory);
        ImmediateScheduler scheduler = new ImmediateScheduler();
        MinionRepository minions = new MinionRepository(database, scheduler);
        minions.initialize().join();
        return new RepositoryContext(
            database,
            minions,
            new MinionStateRepository(database, scheduler),
            new MinionStorageRepository(database, scheduler),
            new MinionUpgradeRepository(database, scheduler),
            new MinionChestLinkRepository(database, scheduler)
        );
    }

    private static MinionData minion(long id, UUID ownerId, String worldKey, int blockX, int blockZ) {
        return new MinionData(
            id,
            ownerId,
            "miner",
            worldKey,
            blockX,
            70,
            blockZ,
            true,
            3,
            5_500L,
            new byte[] {1, 2, 3},
            List.of(
                new StoredItemData(0, new byte[] {4, 5}),
                new StoredItemData(3, new byte[] {6, 7})
            ),
            Map.of("SPEED", 1, "CAPACITY", 2),
            new MinionData.ChestPositionData(worldKey, blockX + 1, 70, blockZ + 1),
            new MinionSettings(MinionDirection.WEST, MiningMode.LINEAR),
            1_000L
        );
    }

    private static long countRows(DatabaseManager database, String table) throws Exception {
        DatabaseConnection connection = database.connectionSource().getReadOnlyConnection("test");
        try {
            return connection.queryForLong("SELECT COUNT(*) FROM " + table);
        }
        finally {
            database.connectionSource().releaseConnection(connection);
        }
    }

    private static void execute(DatabaseManager database, String sql) throws Exception {
        DatabaseConnection connection = database.connectionSource().getReadWriteConnection("test");
        try (Statement statement = connection.getUnderlyingConnection().createStatement()) {
            statement.execute(sql);
        }
        finally {
            database.connectionSource().releaseConnection(connection);
        }
    }

    private static void resetQueryStatistics(DatabaseManager database) throws Exception {
        execute(database, "SET QUERY_STATISTICS FALSE");
        execute(database, "SET QUERY_STATISTICS TRUE");
    }

    private static long minionSelectCount(DatabaseManager database) throws Exception {
        DatabaseConnection connection = database.connectionSource().getReadOnlyConnection("test");
        try (Statement statement = connection.getUnderlyingConnection().createStatement();
             ResultSet rows = statement.executeQuery("""
                 SELECT COALESCE(SUM(EXECUTION_COUNT), 0)
                 FROM INFORMATION_SCHEMA.QUERY_STATISTICS
                 WHERE UPPER(SQL_STATEMENT) LIKE 'SELECT%'
                   AND UPPER(SQL_STATEMENT) LIKE '%ETERNAL_MINION%'
                 """)) {
            rows.next();
            return rows.getLong(1);
        }
        finally {
            database.connectionSource().releaseConnection(connection);
        }
    }

    private record RepositoryContext(
        DatabaseManager database,
        MinionRepository minions,
        MinionStateRepository states,
        MinionStorageRepository storage,
        MinionUpgradeRepository upgrades,
        MinionChestLinkRepository chests
    ) {
    }

    public static final class FailingStorageTrigger implements Trigger {

        @Override
        public void fire(Connection connection, Object[] oldRow, Object[] newRow) {
            throw new IllegalStateException("storage insert rejected");
        }
    }
}
