package com.eternalcode.minions.minion.storage;

import com.eternalcode.minions.database.DatabaseManager;
import com.eternalcode.minions.database.repository.AbstractRepositoryOrmLite;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.minions.minion.MinionId;
import java.util.concurrent.CompletableFuture;

public final class MinionSettingsRepository extends AbstractRepositoryOrmLite {

    public MinionSettingsRepository(DatabaseManager databaseManager, Scheduler scheduler) {
        super(databaseManager, scheduler);
    }

    public CompletableFuture<Void> initialize() {
        return this.createTable(MinionSettingsTable.class);
    }

    public CompletableFuture<Void> saveSettings(MinionId minionId, MinionSettings settings) {
        if (minionId == null || settings == null) {
            throw new IllegalArgumentException("Minion id and settings are required");
        }

        MinionSettingsTable row = new MinionSettingsTable(
                minionId.value(),
                settings.direction().name()
        );
        return this.save(MinionSettingsTable.class, row).thenApply(status -> null);
    }
}
