package com.eternalcode.minions.database.repository;

import com.eternalcode.minions.database.DatabaseManager;
import com.eternalcode.minions.database.table.MinionStateTable;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.minions.minion.MinionId;
import java.util.concurrent.CompletableFuture;

public final class MinionStateRepository extends AbstractRepositoryOrmLite {

    public MinionStateRepository(DatabaseManager databaseManager, Scheduler scheduler) {
        super(databaseManager, scheduler);
    }

    public CompletableFuture<Void> initialize() {
        return this.createTable(MinionStateTable.class);
    }

    private static void validate(MinionId minionId, int level, long progress, long updatedAt) {
        if (minionId == null) {
            throw new IllegalArgumentException("Minion id is required");
        }
        if (level < 0) {
            throw new IllegalArgumentException("Minion level cannot be negative");
        }
        if (progress < 0L) {
            throw new IllegalArgumentException("Minion progress cannot be negative");
        }
        if (updatedAt < 0L) {
            throw new IllegalArgumentException("Minion update time cannot be negative");
        }
    }

    public CompletableFuture<Void> saveState(
            MinionId minionId,
            int level,
            long progress,
            long updatedAt
    ) {
        validate(minionId, level, progress, updatedAt);
        MinionStateTable state = new MinionStateTable(minionId.value(), level, progress, updatedAt);
        return this.save(MinionStateTable.class, state).thenApply(status -> null);
    }
}
