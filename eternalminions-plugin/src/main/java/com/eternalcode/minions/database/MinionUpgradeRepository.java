package com.eternalcode.minions.database;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.minions.minion.MinionId;
import com.eternalcode.minions.minion.upgrade.UpgradeKind;
import java.util.concurrent.CompletableFuture;

public final class MinionUpgradeRepository extends MinionComponentRepository {

    public MinionUpgradeRepository(DatabaseManager databaseManager, Scheduler scheduler) {
        super(databaseManager, scheduler);
    }

    private static void validate(MinionId minionId, UpgradeKind upgrade, int tier) {
        if (minionId == null || upgrade == null) {
            throw new IllegalArgumentException("Minion id and upgrade type are required");
        }
        if (tier < 0) {
            throw new IllegalArgumentException("Upgrade tier cannot be negative");
        }
    }

    public CompletableFuture<Void> saveUpgrade(MinionId minionId, UpgradeKind upgrade, int tier) {
        validate(minionId, upgrade, tier);
        return this.saveComponent(
                MinionUpgradeTable.class,
                MinionUpgradeTable.MINION_ID_COLUMN,
                minionId,
                MinionUpgradeTable.TYPE_COLUMN,
                upgrade.key(),
                MinionUpgradeTable.TIER_COLUMN,
                tier,
                () -> new MinionUpgradeTable(minionId.value(), upgrade.key(), tier)
        );
    }

    public CompletableFuture<Void> deleteUpgrade(MinionId minionId, UpgradeKind upgrade) {
        if (minionId == null || upgrade == null) {
            throw new IllegalArgumentException("Minion id and upgrade type are required");
        }

        return this.deleteComponent(
                MinionUpgradeTable.class,
                MinionUpgradeTable.MINION_ID_COLUMN,
                minionId,
                MinionUpgradeTable.TYPE_COLUMN,
                upgrade.key()
        );
    }
}
