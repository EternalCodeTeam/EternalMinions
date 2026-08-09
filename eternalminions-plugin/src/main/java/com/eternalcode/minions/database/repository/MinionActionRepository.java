package com.eternalcode.minions.database.repository;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.minions.database.DatabaseManager;
import com.eternalcode.minions.database.MinionActionUpdate;
import com.eternalcode.minions.database.table.MinionEquipmentTable;
import com.eternalcode.minions.database.table.MinionStateTable;
import com.eternalcode.minions.minion.storage.MinionStorageTable;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.UpdateBuilder;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class MinionActionRepository extends AbstractRepositoryOrmLite {

    public MinionActionRepository(DatabaseManager databaseManager, Scheduler scheduler) {
        super(databaseManager, scheduler);
    }

    public CompletableFuture<Void> save(MinionActionUpdate update) {
        if (update == null || update.isEmpty()) {
            throw new IllegalArgumentException("Non-empty minion action update is required");
        }

        return this.<MinionStateTable, Long, Void>action(MinionStateTable.class, ignored -> {
            TransactionManager.callInTransaction(this.databaseManager.connectionSource(), () -> {
                this.saveState(update);
                this.saveEquipment(update);
                this.saveStorage(update);
                return null;
            });
            return null;
        });
    }

    private void saveState(MinionActionUpdate update) throws SQLException {
        MinionActionUpdate.StateChange state = update.state();
        if (state == null) {
            return;
        }

        Dao<MinionStateTable, Long> states = this.databaseManager.getDao(MinionStateTable.class);
        states.createOrUpdate(new MinionStateTable(
                update.minionId(),
                state.level(),
                state.progress(),
                state.updatedAt()
        ));
    }

    private void saveEquipment(MinionActionUpdate update) throws SQLException {
        if (update.equipment().isEmpty()) {
            return;
        }

        Dao<MinionEquipmentTable, Object> equipment = this.databaseManager.getDao(MinionEquipmentTable.class);
        for (MinionActionUpdate.EquipmentChange change : update.equipment()) {
            String slot = change.slot().name();
            byte[] serializedItem = change.serializedItem();
            this.saveComponent(
                    equipment,
                    MinionEquipmentTable.MINION_ID_COLUMN,
                    update.minionId(),
                    MinionEquipmentTable.SLOT_COLUMN,
                    slot,
                    MinionEquipmentTable.ITEM_COLUMN,
                    serializedItem,
                    () -> new MinionEquipmentTable(update.minionId(), slot, serializedItem)
            );
        }
    }

    private void saveStorage(MinionActionUpdate update) throws SQLException {
        if (update.storage().isEmpty()) {
            return;
        }

        Dao<MinionStorageTable, Object> storage = this.databaseManager.getDao(MinionStorageTable.class);
        for (MinionActionUpdate.StorageChange change : update.storage()) {
            int slot = change.slot();
            byte[] serializedItem = change.serializedItem();
            this.saveComponent(
                    storage,
                    MinionStorageTable.MINION_ID_COLUMN,
                    update.minionId(),
                    MinionStorageTable.SLOT_COLUMN,
                    slot,
                    MinionStorageTable.ITEM_COLUMN,
                    serializedItem,
                    () -> new MinionStorageTable(update.minionId(), slot, serializedItem)
            );
        }
    }

    private <T> void saveComponent(
            Dao<T, Object> components,
            String minionIdColumn,
            long minionId,
            String componentColumn,
            Object component,
            String valueColumn,
            byte[] value,
            Supplier<T> newRow
    ) throws SQLException {
        if (value.length == 0) {
            DeleteBuilder<T, Object> delete = components.deleteBuilder();
            delete.where()
                    .eq(minionIdColumn, minionId)
                    .and()
                    .eq(componentColumn, component);
            delete.delete();
            return;
        }

        UpdateBuilder<T, Object> update = components.updateBuilder();
        update.updateColumnValue(valueColumn, value);
        update.where()
                .eq(minionIdColumn, minionId)
                .and()
                .eq(componentColumn, component);
        if (update.update() == 0) {
            components.create(newRow.get());
        }
    }
}
