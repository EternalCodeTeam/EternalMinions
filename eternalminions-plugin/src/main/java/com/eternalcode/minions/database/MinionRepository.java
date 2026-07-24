package com.eternalcode.minions.database;

import com.eternalcode.minions.minion.MiningMode;
import com.eternalcode.minions.minion.MinionDirection;
import com.eternalcode.minions.minion.MinionSettings;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.table.TableUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class MinionRepository {

    private final MinionDatabase database;
    private final Executor ioExecutor;
    private Dao<MinionTable, Long> minions;
    private Dao<MinionProgressTable, Long> progress;
    private Dao<MinionEquipmentTable, Long> equipment;
    private Dao<MinionStorageTable, Long> storage;
    private Dao<MinionUpgradeTable, Long> upgrades;
    private Dao<MinionChestTable, Long> chests;
    private Dao<MinionSettingsTable, Long> settings;
    private volatile boolean ready;

    public MinionRepository(MinionDatabase database, Executor ioExecutor) {
        this.database = database;
        this.ioExecutor = ioExecutor;
    }

    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            try {
                this.database.connect();
                TableUtils.createTableIfNotExists(this.database.connectionSource(), MinionTable.class);
                TableUtils.createTableIfNotExists(this.database.connectionSource(), MinionProgressTable.class);
                TableUtils.createTableIfNotExists(this.database.connectionSource(), MinionEquipmentTable.class);
                TableUtils.createTableIfNotExists(this.database.connectionSource(), MinionStorageTable.class);
                TableUtils.createTableIfNotExists(this.database.connectionSource(), MinionUpgradeTable.class);
                TableUtils.createTableIfNotExists(this.database.connectionSource(), MinionChestTable.class);
                TableUtils.createTableIfNotExists(this.database.connectionSource(), MinionSettingsTable.class);
                this.minions = DaoManager.createDao(this.database.connectionSource(), MinionTable.class);
                this.progress = DaoManager.createDao(this.database.connectionSource(), MinionProgressTable.class);
                this.equipment = DaoManager.createDao(this.database.connectionSource(), MinionEquipmentTable.class);
                this.storage = DaoManager.createDao(this.database.connectionSource(), MinionStorageTable.class);
                this.upgrades = DaoManager.createDao(this.database.connectionSource(), MinionUpgradeTable.class);
                this.chests = DaoManager.createDao(this.database.connectionSource(), MinionChestTable.class);
                this.settings = DaoManager.createDao(this.database.connectionSource(), MinionSettingsTable.class);
                this.migrateProgressColumn();
                this.ready = true;
            }
            catch (Exception exception) {
                throw new MinionRepositoryException("Unable to initialize minion database", exception);
            }
        }, this.ioExecutor);
    }

    // Older databases predate the progress column; createTableIfNotExists never alters existing tables.
    private void migrateProgressColumn() throws Exception {
        try {
            this.progress.queryRaw("SELECT progress FROM eternal_minion_progress WHERE 1 = 0").getResults();
        }
        catch (Exception missingColumn) {
            this.progress.executeRawNoArgs("ALTER TABLE eternal_minion_progress ADD COLUMN progress BIGINT NOT NULL DEFAULT 0");
        }
    }

    public CompletableFuture<List<MinionData>> loadAll() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<Long, MinionProgressTable> levels = new HashMap<>();
                for (MinionProgressTable row : this.progress.queryForAll()) {
                    levels.put(row.minionId(), row);
                }
                Map<Long, byte[]> tools = new HashMap<>();
                for (MinionEquipmentTable row : this.equipment.queryForAll()) {
                    if (row.slot().equals("TOOL")) {
                        tools.put(row.minionId(), row.serializedItem());
                    }
                }
                Map<Long, List<StoredItemData>> storedItems = new HashMap<>();
                for (MinionStorageTable row : this.storage.queryForAll()) {
                    storedItems.computeIfAbsent(row.minionId(), ignored -> new ArrayList<>())
                        .add(new StoredItemData(row.slot(), row.serializedItem()));
                }
                Map<Long, Map<String, Integer>> upgradeTiers = new HashMap<>();
                for (MinionUpgradeTable row : this.upgrades.queryForAll()) {
                    upgradeTiers.computeIfAbsent(row.minionId(), ignored -> new HashMap<>())
                        .put(row.kind(), row.tier());
                }
                Map<Long, MinionData.ChestPositionData> chestPositions = new HashMap<>();
                for (MinionChestTable row : this.chests.queryForAll()) {
                    chestPositions.put(
                        row.minionId(),
                        new MinionData.ChestPositionData(row.worldKey(), row.blockX(), row.blockY(), row.blockZ())
                    );
                }
                Map<Long, MinionSettings> minionSettings = new HashMap<>();
                for (MinionSettingsTable row : this.settings.queryForAll()) {
                    minionSettings.put(row.minionId(), parseSettings(row));
                }

                List<MinionData> loaded = new ArrayList<>();
                for (MinionTable row : this.minions.queryForAll()) {
                    MinionProgressTable progressRow = levels.get(row.id());
                    if (progressRow == null) {
                        throw new MinionRepositoryException("Missing progress row for minion " + row.id());
                    }
                    loaded.add(new MinionData(
                        row.id(), row.ownerId(), row.behaviorId(), row.worldKey(), row.blockX(), row.blockY(), row.blockZ(),
                        row.active(), progressRow.level(), progressRow.progress(), tools.getOrDefault(row.id(), new byte[0]),
                        storedItems.getOrDefault(row.id(), List.of()),
                        upgradeTiers.getOrDefault(row.id(), Map.of()),
                        chestPositions.get(row.id()),
                        minionSettings.getOrDefault(row.id(), MinionSettings.defaults())
                    ));
                }
                return List.copyOf(loaded);
            }
            catch (MinionRepositoryException exception) {
                throw exception;
            }
            catch (Exception exception) {
                throw new MinionRepositoryException("Unable to load minions", exception);
            }
        }, this.ioExecutor);
    }

    public CompletableFuture<Void> save(Collection<MinionData> minionData) {
        if (!this.ready || minionData.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        List<MinionData> batch = List.copyOf(minionData);
        return CompletableFuture.runAsync(() -> {
            try {
                this.minions.callBatchTasks(() -> {
                    for (MinionData minion : batch) {
                        this.save(minion);
                    }
                    return null;
                });
            }
            catch (Exception exception) {
                throw new MinionRepositoryException("Unable to save minions", exception);
            }
        }, this.ioExecutor);
    }

    public CompletableFuture<Void> delete(long minionId) {
        if (!this.ready) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.runAsync(() -> {
            try {
                this.minions.callBatchTasks(() -> {
                    this.deleteByMinionId(this.storage, MinionStorageTable.MINION_ID_COLUMN, minionId);
                    this.deleteByMinionId(this.equipment, MinionEquipmentTable.MINION_ID_COLUMN, minionId);
                    this.deleteByMinionId(this.upgrades, MinionUpgradeTable.MINION_ID_COLUMN, minionId);
                    this.chests.deleteById(minionId);
                    this.settings.deleteById(minionId);
                    this.progress.deleteById(minionId);
                    this.minions.deleteById(minionId);
                    return null;
                });
            }
            catch (Exception exception) {
                throw new MinionRepositoryException("Unable to delete minion " + minionId, exception);
            }
        }, this.ioExecutor);
    }

    public boolean ready() {
        return this.ready;
    }

    private void save(MinionData minion) throws Exception {
        this.minions.createOrUpdate(new MinionTable(minion));
        this.progress.createOrUpdate(new MinionProgressTable(minion.id(), minion.level(), minion.progress()));
        this.deleteByMinionId(this.equipment, MinionEquipmentTable.MINION_ID_COLUMN, minion.id());
        if (minion.serializedTool().length > 0) {
            this.equipment.create(new MinionEquipmentTable(minion.id(), "TOOL", minion.serializedTool()));
        }
        this.deleteByMinionId(this.storage, MinionStorageTable.MINION_ID_COLUMN, minion.id());
        for (StoredItemData storedItem : minion.storageItems()) {
            this.storage.create(new MinionStorageTable(minion.id(), storedItem.slot(), storedItem.serializedItem()));
        }
        this.deleteByMinionId(this.upgrades, MinionUpgradeTable.MINION_ID_COLUMN, minion.id());
        for (Map.Entry<String, Integer> upgrade : minion.upgrades().entrySet()) {
            this.upgrades.create(new MinionUpgradeTable(minion.id(), upgrade.getKey(), upgrade.getValue()));
        }
        MinionData.ChestPositionData chest = minion.chestPosition();
        if (chest == null) {
            this.chests.deleteById(minion.id());
        }
        else {
            this.chests.createOrUpdate(
                new MinionChestTable(minion.id(), chest.worldKey(), chest.blockX(), chest.blockY(), chest.blockZ()));
        }
        this.settings.createOrUpdate(new MinionSettingsTable(
            minion.id(), minion.settings().direction().name(), minion.settings().miningMode().name()));
    }

    // Values written by a newer plugin version fall back to defaults instead of failing the load.
    private static MinionSettings parseSettings(MinionSettingsTable row) {
        MinionSettings parsed = MinionSettings.defaults();
        try {
            parsed = parsed.withDirection(MinionDirection.valueOf(row.direction()));
        }
        catch (IllegalArgumentException ignored) {
        }
        try {
            parsed = parsed.withMiningMode(MiningMode.valueOf(row.miningMode()));
        }
        catch (IllegalArgumentException ignored) {
        }
        return parsed;
    }

    private <T> void deleteByMinionId(Dao<T, Long> dao, String columnName, long minionId) throws Exception {
        DeleteBuilder<T, Long> delete = dao.deleteBuilder();
        delete.where().eq(columnName, minionId);
        delete.delete();
    }
}
