package com.eternalcode.minions.database;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.minions.minion.MinionId;
import java.util.concurrent.CompletableFuture;

public final class MinionEquipmentRepository extends MinionComponentRepository {

    public MinionEquipmentRepository(DatabaseManager databaseManager, Scheduler scheduler) {
        super(databaseManager, scheduler);
    }

    private static void validate(MinionId minionId, MinionEquipmentSlot slot, byte[] serializedItem) {
        if (minionId == null || slot == null) {
            throw new IllegalArgumentException("Minion id and equipment slot are required");
        }
        if (serializedItem == null || serializedItem.length == 0) {
            throw new IllegalArgumentException("Serialized equipment item cannot be empty");
        }
    }

    public CompletableFuture<Void> saveSlot(MinionId minionId, MinionEquipmentSlot slot, byte[] serializedItem) {
        validate(minionId, slot, serializedItem);
        byte[] itemSnapshot = serializedItem.clone();
        return this.saveComponent(
                MinionEquipmentTable.class,
                MinionEquipmentTable.MINION_ID_COLUMN,
                minionId,
                MinionEquipmentTable.SLOT_COLUMN,
                slot.name(),
                MinionEquipmentTable.ITEM_COLUMN,
                itemSnapshot,
                () -> new MinionEquipmentTable(minionId.value(), slot.name(), itemSnapshot)
        );
    }

    public CompletableFuture<Void> deleteSlot(MinionId minionId, MinionEquipmentSlot slot) {
        if (minionId == null || slot == null) {
            throw new IllegalArgumentException("Minion id and equipment slot are required");
        }

        return this.deleteComponent(
                MinionEquipmentTable.class,
                MinionEquipmentTable.MINION_ID_COLUMN,
                minionId,
                MinionEquipmentTable.SLOT_COLUMN,
                slot.name()
        );
    }
}
