package com.eternalcode.minions.database.repository;

import com.eternalcode.minions.database.DatabaseManager;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.minions.minion.MinionId;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.UpdateBuilder;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public abstract class MinionComponentRepository extends AbstractRepositoryOrmLite {

    protected MinionComponentRepository(DatabaseManager databaseManager, Scheduler scheduler) {
        super(databaseManager, scheduler);
    }

    protected <T> CompletableFuture<Void> saveComponent(
            Class<T> tableType,
            String minionIdColumn,
            MinionId minionId,
            String componentColumn,
            Object component,
            String valueColumn,
            Object value,
            Supplier<T> newRow
    ) {
        if (tableType == null || minionIdColumn == null || minionId == null
                || componentColumn == null || component == null || valueColumn == null
                || value == null || newRow == null) {
            throw new IllegalArgumentException("Component persistence arguments are required");
        }

        return this.action(tableType, components -> {
            UpdateBuilder<T, Object> update = components.updateBuilder();
            update.updateColumnValue(valueColumn, value);
            update.where()
                    .eq(minionIdColumn, minionId.value())
                    .and()
                    .eq(componentColumn, component);
            if (update.update() == 0) {
                components.create(newRow.get());
            }
            return null;
        });
    }

    protected <T> CompletableFuture<Void> deleteComponent(
            Class<T> tableType,
            String minionIdColumn,
            MinionId minionId,
            String componentColumn,
            Object component
    ) {
        if (tableType == null || minionIdColumn == null || minionId == null
                || componentColumn == null || component == null) {
            throw new IllegalArgumentException("Component deletion arguments are required");
        }

        return this.action(tableType, components -> {
            DeleteBuilder<T, Object> delete = components.deleteBuilder();
            delete.where()
                    .eq(minionIdColumn, minionId.value())
                    .and()
                    .eq(componentColumn, component);
            delete.delete();
            return null;
        });
    }
}
