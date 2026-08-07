package com.eternalcode.minions.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eternalcode.minions.database.repository.MinionEquipmentRepository;
import com.eternalcode.minions.database.repository.MinionRepository;
import com.eternalcode.minions.database.repository.MinionRepositoryException;
import com.eternalcode.minions.database.repository.MinionStateRepository;
import com.eternalcode.minions.database.table.MinionStateTable;
import com.eternalcode.minions.minion.MinionDirection;
import com.eternalcode.minions.minion.MinionId;
import com.eternalcode.minions.minion.storage.MinionChestLinkRepository;
import com.eternalcode.minions.minion.storage.MinionSettings;
import com.eternalcode.minions.minion.storage.MinionSettingsRepository;
import com.eternalcode.minions.minion.storage.MinionStorageRepository;
import com.eternalcode.minions.minion.upgrade.DefaultUpgradeKinds;
import com.eternalcode.minions.minion.upgrade.MinionUpgradeRepository;
import com.j256.ormlite.dao.Dao;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Contract shared by every real database engine the plugin can be configured with. Run against
 * a real Postgres and a real MySQL via Testcontainers (see the two concrete subclasses) - this is
 * about SQL dialect and driver correctness, not about our own in-memory logic, so it must not run
 * against H2/SQLite as a stand-in for either.
 */
abstract class AbstractMinionRepositoryContractTest {

    private static final UUID OWNER_A = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OWNER_B = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @TempDir
    File tempDir;

    private DatabaseManager databaseManager;
    private DatabaseScheduler scheduler;
    private MinionRepository minions;

    protected abstract DatabaseSettings settings();

    @BeforeEach
    void connect() {
        this.scheduler = new DatabaseScheduler("minion-repository-test");
        this.databaseManager = new DatabaseManager(Logger.getAnonymousLogger(), this.tempDir, this.settings());
        this.databaseManager.connect();
        this.minions = new MinionRepository(this.databaseManager, this.scheduler);
        this.createAllTables();
    }

    @AfterEach
    void disconnect() {
        this.scheduler.close();
        this.databaseManager.close();
    }

    @Test
    void shouldPersistAndReloadAFullMinionAggregate() {
        MinionData original = minionData(1, OWNER_A, 5, 2);

        await(this.minions.create(original));
        MinionData loaded = await(this.minions.findById(new MinionId(1))).orElseThrow();

        assertThat(loaded.ownerId()).isEqualTo(OWNER_A);
        assertThat(loaded.behaviorId()).isEqualTo("MINER");
        assertThat(loaded.level()).isEqualTo(5);
        assertThat(loaded.progress()).isEqualTo(1_000L);
        assertThat(loaded.serializedTool()).isEqualTo("diamond-pickaxe".getBytes(StandardCharsets.UTF_8));
        assertThat(loaded.storageItems()).hasSize(2);
        assertThat(loaded.upgrades()).containsEntry(DefaultUpgradeKinds.SPEED.key(), 2);
        assertThat(loaded.chestPosition().blockX()).isEqualTo(5);
        assertThat(loaded.settings().direction()).isEqualTo(MinionDirection.EAST);
    }

    @Test
    void shouldReturnEmptyWhenMinionDoesNotExist() {
        Optional<MinionData> result = await(this.minions.findById(new MinionId(999)));

        assertThat(result).isEmpty();
    }

    @Test
    void shouldRejectCreatingASecondMinionWithAnAlreadyUsedId() {
        await(this.minions.create(minionData(2, OWNER_A, 1, 1)));

        assertThatThrownBy(() -> await(this.minions.create(minionData(2, OWNER_A, 1, 1))))
                .as("the id column's primary key constraint must reject a duplicate insert")
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldDeleteAMinionAndAllOfItsComponentRows() {
        await(this.minions.create(minionData(3, OWNER_A, 1, 1)));

        await(this.minions.deleteMinion(new MinionId(3)));

        assertThat(await(this.minions.findById(new MinionId(3)))).isEmpty();
        assertThat(this.countRows(MinionStateTable.class, 3)).isZero();
    }

    @Test
    void shouldFindOnlyMinionsOwnedByTheRequestedPlayer() {
        await(this.minions.create(minionData(10, OWNER_A, 1, 1)));
        await(this.minions.create(minionData(11, OWNER_A, 1, 1)));
        await(this.minions.create(minionData(12, OWNER_B, 1, 1)));

        List<MinionData> ownerAMinions = await(this.minions.findByOwner(OWNER_A));

        assertThat(ownerAMinions).extracting(MinionData::id).containsExactlyInAnyOrder(10L, 11L);
    }

    @Test
    void shouldFindMinionsOnlyWithinTheRequestedChunk() {
        await(this.minions.create(minionDataAt(20, 3, 64, 3)));
        await(this.minions.create(minionDataAt(21, 5, 70, 12)));
        await(this.minions.create(minionDataAt(22, 20, 64, 3)));

        List<MinionData> inChunkZero = await(this.minions.findByChunk("world", 0, 0));

        assertThat(inChunkZero).extracting(MinionData::id).containsExactlyInAnyOrder(20L, 21L);
    }

    @Test
    void shouldFailLoadingAllMinionsWhenOnlyOneHasACorruptedMissingStateRow() throws Exception {
        // Regression coverage for a real resilience gap: MinionQueryRepository.assemble() throws
        // for any minion whose state row is missing, and that exception propagates out of the
        // batched load() call - so one corrupted/orphaned minion currently fails loadAll() for
        // every minion, not just the broken one.
        await(this.minions.create(minionData(30, OWNER_A, 1, 1)));
        await(this.minions.create(minionData(31, OWNER_A, 1, 1)));
        this.deleteStateRowDirectly(31);

        CompletableFuture<List<MinionData>> future = this.minions.loadAll();

        assertThatThrownBy(() -> await(future))
                .as("a single corrupted minion currently takes down loadAll() for every minion")
                .hasCauseInstanceOf(MinionRepositoryException.class);
    }

    @Test
    void shouldCorrectlyBatchQueryComponentRowsAcrossTheInParameterLimit() {
        int minionCount = 520; // exceeds MinionQueryRepository.MAXIMUM_IN_PARAMETERS (500)
        for (long id = 1; id <= minionCount; id++) {
            await(this.minions.create(minionData(id, OWNER_A, 1, 1)));
        }

        List<MinionData> loaded = await(this.minions.loadAll());

        assertThat(loaded).hasSize(minionCount);
        assertThat(loaded).allSatisfy(minion -> assertThat(minion.level()).isEqualTo(5));
    }

    @Test
    void shouldUpsertAnEquipmentSlotInsteadOfInsertingADuplicateRow() {
        await(this.minions.create(minionData(40, OWNER_A, 1, 1)));
        MinionEquipmentRepository equipment = new MinionEquipmentRepository(this.databaseManager, this.scheduler);
        MinionId minionId = new MinionId(40);

        await(equipment.saveSlot(minionId, MinionEquipmentSlot.TOOL, "wooden-pickaxe".getBytes(StandardCharsets.UTF_8)));
        await(equipment.saveSlot(minionId, MinionEquipmentSlot.TOOL, "diamond-pickaxe".getBytes(StandardCharsets.UTF_8)));

        MinionData reloaded = await(this.minions.findById(minionId)).orElseThrow();
        assertThat(reloaded.serializedTool()).isEqualTo("diamond-pickaxe".getBytes(StandardCharsets.UTF_8));
        assertThat(this.countRows(com.eternalcode.minions.database.table.MinionEquipmentTable.class, 40))
                .as("saveSlot must update the existing row, not accumulate duplicates")
                .isEqualTo(1);
    }

    @Test
    void shouldSurviveRepositoryRecreationLikeAServerRestart() {
        await(this.minions.create(minionData(50, OWNER_A, 3, 2)));

        // A fresh DatabaseManager + MinionRepository pointed at the same database, simulating the
        // plugin being fully restarted rather than reusing the same in-memory objects.
        DatabaseManager restartedManager =
                new DatabaseManager(Logger.getAnonymousLogger(), this.tempDir, this.settings());
        restartedManager.connect();
        MinionRepository restartedRepository = new MinionRepository(restartedManager, this.scheduler);

        MinionData reloaded = await(restartedRepository.findById(new MinionId(50))).orElseThrow();

        assertThat(reloaded.level()).isEqualTo(3);
        restartedManager.close();
    }

    private void createAllTables() {
        MinionStateRepository states = new MinionStateRepository(this.databaseManager, this.scheduler);
        MinionSettingsRepository settings = new MinionSettingsRepository(this.databaseManager, this.scheduler);
        MinionEquipmentRepository equipment = new MinionEquipmentRepository(this.databaseManager, this.scheduler);
        MinionStorageRepository storage = new MinionStorageRepository(this.databaseManager, this.scheduler);
        MinionUpgradeRepository upgrades = new MinionUpgradeRepository(this.databaseManager, this.scheduler);
        MinionChestLinkRepository chestLinks = new MinionChestLinkRepository(this.databaseManager, this.scheduler);

        await(CompletableFuture.allOf(
                this.minions.initialize(),
                states.initialize(),
                settings.initialize(),
                equipment.initialize(),
                storage.initialize(),
                upgrades.initialize(),
                chestLinks.initialize()
        ));
    }

    private void deleteStateRowDirectly(long minionId) throws Exception {
        Dao<MinionStateTable, Long> dao = this.databaseManager.<MinionStateTable, Long>getDao(MinionStateTable.class);
        dao.deleteById(minionId);
    }

    @SuppressWarnings("unchecked")
    private int countRows(Class<?> tableType, long minionId) {
        try {
            Dao<Object, Object> dao = this.databaseManager.getDao((Class<Object>) tableType);
            return dao.queryForFieldValues(Map.of("minion_id", minionId)).size();
        }
        catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static MinionData minionData(long id, UUID owner, int level, int upgradeTier) {
        return minionDataAt(id, owner, level, upgradeTier, "world", 5, 64, 5);
    }

    private static MinionData minionDataAt(long id, int blockX, int blockY, int blockZ) {
        return minionDataAt(id, OWNER_A, 5, 2, "world", blockX, blockY, blockZ);
    }

    private static MinionData minionDataAt(
            long id, UUID owner, int level, int upgradeTier, String worldKey, int blockX, int blockY, int blockZ
    ) {
        return new MinionData(
                id,
                owner,
                "MINER",
                worldKey,
                blockX,
                blockY,
                blockZ,
                level,
                1_000L,
                "diamond-pickaxe".getBytes(StandardCharsets.UTF_8),
                List.of(
                        new StoredItemData(0, "cobblestone-stack".getBytes(StandardCharsets.UTF_8)),
                        new StoredItemData(3, "diamond-stack".getBytes(StandardCharsets.UTF_8))
                ),
                Map.of(DefaultUpgradeKinds.SPEED.key(), upgradeTier),
                new MinionData.ChestPositionData(worldKey, blockX, blockY, blockZ),
                new MinionSettings(MinionDirection.EAST)
        );
    }

    private static <T> T await(CompletableFuture<T> future) {
        try {
            return future.get(10, TimeUnit.SECONDS);
        }
        catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(cause);
        }
        catch (InterruptedException | TimeoutException exception) {
            throw new RuntimeException(exception);
        }
    }
}
