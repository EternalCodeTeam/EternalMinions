package com.eternalcode.minions.database;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.minions.minion.MinionId;
import com.eternalcode.minions.minion.storage.MinionSettings;
import java.util.concurrent.CompletableFuture;

public final class MinionSettingsRepository extends AbstractRepositoryOrmLite {

    public MinionSettingsRepository(DatabaseManager databaseManager, Scheduler scheduler) {
        super(databaseManager, scheduler);
    }

    public CompletableFuture<Void> saveSettings(MinionId minionId, MinionSettings settings) {
        if (minionId == null || settings == null) {
            throw new IllegalArgumentException("Minion id and settings are required");
        }

        MinionSettingsTable row = new MinionSettingsTable(
                minionId.value(),
                settings.direction().name(),
                settings.miningMode().name()
        );
        return this.save(MinionSettingsTable.class, row).thenApply(status -> null);
    }
}
