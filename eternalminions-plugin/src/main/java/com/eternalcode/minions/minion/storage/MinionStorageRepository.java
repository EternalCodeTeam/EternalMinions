package com.eternalcode.minions.minion.storage;

import com.eternalcode.minions.database.DatabaseManager;
import com.eternalcode.minions.database.repository.MinionComponentRepository;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.minions.minion.MinionId;
import java.util.concurrent.CompletableFuture;

public final class MinionStorageRepository extends MinionComponentRepository {

    public MinionStorageRepository(DatabaseManager databaseManager, Scheduler scheduler) {
        super(databaseManager, scheduler);
    }

    public CompletableFuture<Void> initialize() {
        return this.createTable(MinionStorageTable.class);
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
        return this.saveComponent(
                MinionStorageTable.class,
                MinionStorageTable.MINION_ID_COLUMN,
                minionId,
                MinionStorageTable.SLOT_COLUMN,
                slot,
                MinionStorageTable.ITEM_COLUMN,
                itemSnapshot,
                () -> new MinionStorageTable(minionId.value(), slot, itemSnapshot)
        );
    }

    public CompletableFuture<Void> deleteSlot(MinionId minionId, int slot) {
        if (minionId == null) {
            throw new IllegalArgumentException("Minion id is required");
        }
        if (slot < 0) {
            throw new IllegalArgumentException("Storage slot cannot be negative");
        }

        return this.deleteComponent(
                MinionStorageTable.class,
                MinionStorageTable.MINION_ID_COLUMN,
                minionId,
                MinionStorageTable.SLOT_COLUMN,
                slot
        );
    }
}
