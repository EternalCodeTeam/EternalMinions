package com.eternalcode.minions.database;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.minions.minion.MinionId;
import com.eternalcode.minions.minion.MinionUpgradeKind;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.UpdateBuilder;
import java.util.concurrent.CompletableFuture;

public final class MinionUpgradeRepository extends AbstractRepositoryOrmLite {

    public MinionUpgradeRepository(DatabaseManager databaseManager, Scheduler scheduler) {
        super(databaseManager, scheduler);
    }

    private static void validate(MinionId minionId, MinionUpgradeKind upgrade, int tier) {
        if (minionId == null || upgrade == null) {
            throw new IllegalArgumentException("Minion id and upgrade type are required");
        }
        if (tier < 0) {
            throw new IllegalArgumentException("Upgrade tier cannot be negative");
        }
    }

    public CompletableFuture<Void> saveUpgrade(MinionId minionId, MinionUpgradeKind upgrade, int tier) {
        validate(minionId, upgrade, tier);
        return this.action(
                MinionUpgradeTable.class, upgrades -> {
                    UpdateBuilder<MinionUpgradeTable, Object> update = upgrades.updateBuilder();
                    update.updateColumnValue("tier", tier);
                    update.where()
                            .eq(MinionUpgradeTable.MINION_ID_COLUMN, minionId.value())
                            .and()
                            .eq("upgrade_type", upgrade.name());
                    if (update.update() == 0) {
                        upgrades.create(new MinionUpgradeTable(minionId.value(), upgrade.name(), tier));
                    }
                    return null;
                });
    }

    public CompletableFuture<Void> deleteUpgrade(MinionId minionId, MinionUpgradeKind upgrade) {
        if (minionId == null || upgrade == null) {
            throw new IllegalArgumentException("Minion id and upgrade type are required");
        }

        return this.action(
                MinionUpgradeTable.class, upgrades -> {
                    DeleteBuilder<MinionUpgradeTable, Object> delete = upgrades.deleteBuilder();
                    delete.where()
                            .eq(MinionUpgradeTable.MINION_ID_COLUMN, minionId.value())
                            .and()
                            .eq("upgrade_type", upgrade.name());
                    delete.delete();
                    return null;
                });
    }
}
