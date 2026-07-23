package com.eternalcode.minions.database;

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
                this.minions = DaoManager.createDao(this.database.connectionSource(), MinionTable.class);
                this.progress = DaoManager.createDao(this.database.connectionSource(), MinionProgressTable.class);
                this.equipment = DaoManager.createDao(this.database.connectionSource(), MinionEquipmentTable.class);
                this.storage = DaoManager.createDao(this.database.connectionSource(), MinionStorageTable.class);
                this.ready = true;
            }
            catch (Exception exception) {
                throw new MinionRepositoryException("Unable to initialize minion database", exception);
            }
        }, this.ioExecutor);
    }

    public CompletableFuture<List<MinionData>> loadAll() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<Long, Integer> levels = new HashMap<>();
                for (MinionProgressTable row : this.progress.queryForAll()) {
                    levels.put(row.minionId(), row.level());
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

                List<MinionData> loaded = new ArrayList<>();
                for (MinionTable row : this.minions.queryForAll()) {
                    Integer level = levels.get(row.id());
                    if (level == null) {
                        throw new MinionRepositoryException("Missing progress row for minion " + row.id());
                    }
                    loaded.add(new MinionData(
                        row.id(), row.ownerId(), row.behaviorId(), row.worldKey(), row.blockX(), row.blockY(), row.blockZ(),
                        row.active(), level, tools.getOrDefault(row.id(), new byte[0]),
                        storedItems.getOrDefault(row.id(), List.of())
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
        this.progress.createOrUpdate(new MinionProgressTable(minion.id(), minion.level()));
        this.deleteByMinionId(this.equipment, MinionEquipmentTable.MINION_ID_COLUMN, minion.id());
        if (minion.serializedTool().length > 0) {
            this.equipment.create(new MinionEquipmentTable(minion.id(), "TOOL", minion.serializedTool()));
        }
        this.deleteByMinionId(this.storage, MinionStorageTable.MINION_ID_COLUMN, minion.id());
        for (StoredItemData storedItem : minion.storageItems()) {
            this.storage.create(new MinionStorageTable(minion.id(), storedItem.slot(), storedItem.serializedItem()));
        }
    }

    private <T> void deleteByMinionId(Dao<T, Long> dao, String columnName, long minionId) throws Exception {
        DeleteBuilder<T, Long> delete = dao.deleteBuilder();
        delete.where().eq(columnName, minionId);
        delete.delete();
    }
}
