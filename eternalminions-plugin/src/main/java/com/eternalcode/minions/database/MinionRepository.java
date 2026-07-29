package com.eternalcode.minions.database;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.minions.minion.MinionId;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.stmt.DeleteBuilder;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class MinionRepository extends AbstractRepositoryOrmLite {

    private final MinionQueryRepository queries;
    private volatile boolean ready;

    public MinionRepository(DatabaseManager databaseManager, Scheduler scheduler) {
        super(databaseManager, scheduler);
        this.queries = new MinionQueryRepository(databaseManager, scheduler);
    }

    private static void validateAggregate(MinionData minion) {
        Map<Integer, Boolean> storageSlots = new HashMap<>();
        for (StoredItemData storedItem : minion.storageItems()) {
            if (storedItem.serializedItem().length == 0) {
                throw new IllegalArgumentException("Serialized storage item cannot be empty");
            }
            if (storageSlots.put(storedItem.slot(), Boolean.TRUE) != null) {
                throw new IllegalArgumentException("Duplicate storage slot " + storedItem.slot());
            }
        }
        for (Map.Entry<String, Integer> upgrade : minion.upgrades().entrySet()) {
            if (upgrade.getValue() < 0) {
                throw new IllegalArgumentException("Upgrade tier cannot be negative");
            }
        }
    }

    public CompletableFuture<Void> initialize() {
        return this.createTable(MinionTable.class);
    }

    void markReady() {
        this.ready = true;
    }

    public CompletableFuture<Void> create(MinionData minion) {
        if (minion == null) {
            throw new IllegalArgumentException("Minion data is required");
        }
        validateAggregate(minion);

        return this.<MinionTable, Long, Void>action(
                MinionTable.class, minions -> {
                    TransactionManager.callInTransaction(
                            this.databaseManager.connectionSource(), () -> {
                                this.insertAggregate(minions, minion);
                                return null;
                            });
                    return null;
                });
    }

    public CompletableFuture<Optional<MinionData>> findById(MinionId minionId) {
        return this.queries.findById(minionId);
    }

    public CompletableFuture<List<MinionData>> findByOwner(UUID ownerId) {
        return this.queries.findByOwner(ownerId);
    }

    public CompletableFuture<List<MinionData>> findByChunk(String worldKey, int chunkX, int chunkZ) {
        return this.queries.findByChunk(worldKey, chunkX, chunkZ);
    }

    public CompletableFuture<List<MinionData>> loadAll() {
        return this.queries.loadAll();
    }

    public CompletableFuture<Void> deleteMinion(MinionId minionId) {
        if (minionId == null) {
            throw new IllegalArgumentException("Minion id is required");
        }
        return this.<MinionTable, Long, Void>action(
                MinionTable.class,
                minions -> {
                    TransactionManager.callInTransaction(
                            this.databaseManager.connectionSource(),
                            () -> {
                                this.deleteComponents(minionId.value());
                                minions.deleteById(minionId.value());
                                return null;
                            }
                    );
                    return null;
                }
        );
    }

    public boolean ready() {
        return this.ready;
    }

    private void insertAggregate(Dao<MinionTable, Long> minions, MinionData minion) throws Exception {
        minions.create(new MinionTable(minion));
        this.databaseManager.<MinionStateTable, Long>getDao(MinionStateTable.class).create(
                new MinionStateTable(
                        minion.id(),
                        minion.level(),
                        minion.progress(),
                        minion.createdAt()
                )
        );
        this.databaseManager.<MinionSettingsTable, Long>getDao(MinionSettingsTable.class).create(
                new MinionSettingsTable(
                        minion.id(),
                        minion.settings().direction().name()
                )
        );
        this.insertEquipment(minion);
        this.insertStorage(minion);
        this.insertUpgrades(minion);
        this.insertChestLink(minion);
    }

    private void insertEquipment(MinionData minion) throws Exception {
        if (minion.serializedTool().length == 0) {
            return;
        }
        this.databaseManager.getDao(MinionEquipmentTable.class).create(
                new MinionEquipmentTable(minion.id(), MinionEquipmentSlot.TOOL.name(), minion.serializedTool())
        );
    }

    private void insertStorage(MinionData minion) throws Exception {
        Dao<MinionStorageTable, Object> storage = this.databaseManager.getDao(MinionStorageTable.class);
        for (StoredItemData storedItem : minion.storageItems()) {
            storage.create(new MinionStorageTable(minion.id(), storedItem.slot(), storedItem.serializedItem()));
        }
    }

    private void insertUpgrades(MinionData minion) throws Exception {
        Dao<MinionUpgradeTable, Object> upgrades = this.databaseManager.getDao(MinionUpgradeTable.class);
        for (Map.Entry<String, Integer> upgrade : minion.upgrades().entrySet()) {
            if (upgrade.getValue() == 0) {
                continue;
            }
            upgrades.create(new MinionUpgradeTable(minion.id(), upgrade.getKey(), upgrade.getValue()));
        }
    }

    private void insertChestLink(MinionData minion) throws Exception {
        if (minion.chestPosition() == null) {
            return;
        }
        MinionData.ChestPositionData chest = minion.chestPosition();
        this.databaseManager.<MinionChestLinkTable, Long>getDao(MinionChestLinkTable.class).create(
                new MinionChestLinkTable(
                        minion.id(),
                        chest.worldKey(),
                        chest.blockX(),
                        chest.blockY(),
                        chest.blockZ()
                )
        );
    }

    private void deleteComponents(long minionId) throws SQLException {
        this.deleteComponents(MinionStateTable.class, minionId);
        this.deleteComponents(MinionSettingsTable.class, minionId);
        this.deleteComponents(MinionEquipmentTable.class, minionId);
        this.deleteComponents(MinionStorageTable.class, minionId);
        this.deleteComponents(MinionUpgradeTable.class, minionId);
        this.deleteComponents(MinionChestLinkTable.class, minionId);
    }

    private <T> void deleteComponents(Class<T> tableType, long minionId) throws SQLException {
        Dao<T, Object> components = this.databaseManager.getDao(tableType);
        DeleteBuilder<T, Object> delete = components.deleteBuilder();
        delete.where().eq("minion_id", minionId);
        delete.delete();
    }
}
