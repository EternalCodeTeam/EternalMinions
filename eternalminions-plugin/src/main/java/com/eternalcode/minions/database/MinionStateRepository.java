package com.eternalcode.minions.database;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.minions.minion.MinionId;
import java.util.concurrent.CompletableFuture;

public final class MinionStateRepository extends AbstractRepositoryOrmLite {

    public MinionStateRepository(DatabaseManager databaseManager, Scheduler scheduler) {
        super(databaseManager, scheduler);
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
            boolean active,
            int level,
            long progress,
            long updatedAt
    ) {
        validate(minionId, level, progress, updatedAt);
        MinionStateTable state = new MinionStateTable(minionId.value(), active, level, progress, updatedAt);
        return this.save(MinionStateTable.class, state).thenApply(status -> null);
    }
}
