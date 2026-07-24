package com.eternalcode.minions.database;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.minions.minion.MinionId;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.UpdateBuilder;
import java.util.concurrent.CompletableFuture;

public final class MinionStorageRepository extends AbstractRepositoryOrmLite {

    public MinionStorageRepository(DatabaseManager databaseManager, Scheduler scheduler) {
        super(databaseManager, scheduler);
    }

    private static void validate(MinionId minionId, int slot, byte[] serializedItem) {
        if (minionId == null) {
            throw new IllegalArgumentException("Minion id is required");
        }
        if (slot < 0) {
            throw new IllegalArgumentException("Storage slot cannot be negative");
        }
        if (serializedItem == null || serializedItem.length == 0) {
            throw new IllegalArgumentException("Serialized storage item cannot be empty");
        }
    }

    public CompletableFuture<Void> saveSlot(MinionId minionId, int slot, byte[] serializedItem) {
        validate(minionId, slot, serializedItem);
        byte[] itemSnapshot = serializedItem.clone();
        return this.action(
                MinionStorageTable.class, storage -> {
                    UpdateBuilder<MinionStorageTable, Object> update = storage.updateBuilder();
                    update.updateColumnValue("serialized_item", itemSnapshot);
                    update.where()
                            .eq(MinionStorageTable.MINION_ID_COLUMN, minionId.value())
                            .and()
                            .eq("slot", slot);
                    if (update.update() == 0) {
                        storage.create(new MinionStorageTable(minionId.value(), slot, itemSnapshot));
                    }
                    return null;
                });
    }

    public CompletableFuture<Void> deleteSlot(MinionId minionId, int slot) {
        if (minionId == null) {
            throw new IllegalArgumentException("Minion id is required");
        }
        if (slot < 0) {
            throw new IllegalArgumentException("Storage slot cannot be negative");
        }

        return this.action(
                MinionStorageTable.class, storage -> {
                    DeleteBuilder<MinionStorageTable, Object> delete = storage.deleteBuilder();
                    delete.where()
                            .eq(MinionStorageTable.MINION_ID_COLUMN, minionId.value())
                            .and()
                            .eq("slot", slot);
                    delete.delete();
                    return null;
                });
    }
}
