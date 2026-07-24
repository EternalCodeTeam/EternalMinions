package com.eternalcode.minions.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "eternal_minion_upgrades")
final class MinionUpgradeTable {

    static final String MINION_ID_COLUMN = "minion_id";

    @DatabaseField(columnName = MINION_ID_COLUMN) private long minionId;
    @DatabaseField(columnName = "upgrade_type", canBeNull = false) private String upgradeType;
    @DatabaseField(columnName = "tier") private int tier;

    MinionUpgradeTable() {
    }

    MinionUpgradeTable(long minionId, String upgradeType, int tier) {
        this.minionId = minionId;
        this.upgradeType = upgradeType;
        this.tier = tier;
    }

    long minionId() {return this.minionId;}

    String upgradeType() {return this.upgradeType;}

    int tier() {return this.tier;}
}
