package com.eternalcode.minions.database;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.minions.minion.MinionId;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.UpdateBuilder;
import java.util.concurrent.CompletableFuture;

public final class MinionEquipmentRepository extends AbstractRepositoryOrmLite {

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
        return this.action(
                MinionEquipmentTable.class, equipment -> {
                    UpdateBuilder<MinionEquipmentTable, Object> update = equipment.updateBuilder();
                    update.updateColumnValue("serialized_item", itemSnapshot);
                    update.where()
                            .eq(MinionEquipmentTable.MINION_ID_COLUMN, minionId.value())
                            .and()
                            .eq("slot", slot.name());
                    if (update.update() == 0) {
                        equipment.create(new MinionEquipmentTable(minionId.value(), slot.name(), itemSnapshot));
                    }
                    return null;
                });
    }

    public CompletableFuture<Void> deleteSlot(MinionId minionId, MinionEquipmentSlot slot) {
        if (minionId == null || slot == null) {
            throw new IllegalArgumentException("Minion id and equipment slot are required");
        }

        return this.action(
                MinionEquipmentTable.class, equipment -> {
                    DeleteBuilder<MinionEquipmentTable, Object> delete = equipment.deleteBuilder();
                    delete.where()
                            .eq(MinionEquipmentTable.MINION_ID_COLUMN, minionId.value())
                            .and()
                            .eq("slot", slot.name());
                    delete.delete();
                    return null;
                });
    }
}
