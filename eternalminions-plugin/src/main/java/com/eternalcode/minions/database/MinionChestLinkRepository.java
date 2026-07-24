package com.eternalcode.minions.database;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.minions.minion.MinionId;
import com.eternalcode.minions.minion.MinionPosition;
import java.util.concurrent.CompletableFuture;

public final class MinionChestLinkRepository extends AbstractRepositoryOrmLite {

    public MinionChestLinkRepository(DatabaseManager databaseManager, Scheduler scheduler) {
        super(databaseManager, scheduler);
    }

    public CompletableFuture<Void> saveLink(MinionId minionId, MinionPosition position) {
        if (minionId == null || position == null) {
            throw new IllegalArgumentException("Minion id and chest position are required");
        }

        MinionChestLinkTable link = new MinionChestLinkTable(
                minionId.value(),
                position.worldKey(),
                position.blockX(),
                position.blockY(),
                position.blockZ()
        );
        return this.save(MinionChestLinkTable.class, link).thenApply(status -> null);
    }

    public CompletableFuture<Void> deleteLink(MinionId minionId) {
        if (minionId == null) {
            throw new IllegalArgumentException("Minion id is required");
        }
        return this.deleteById(MinionChestLinkTable.class, minionId.value()).thenApply(deleted -> null);
    }
}
