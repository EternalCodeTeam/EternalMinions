package com.eternalcode.minions.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "eternal_minion_upgrades")
final class MinionUpgradeTable {

    static final String MINION_ID_COLUMN = "minion_id";
    static final String TYPE_COLUMN = "upgrade_type";
    static final String TIER_COLUMN = "tier";

    @DatabaseField(columnName = MINION_ID_COLUMN, uniqueCombo = true) private long minionId;
    @DatabaseField(columnName = TYPE_COLUMN, canBeNull = false, uniqueCombo = true) private String upgradeType;
    @DatabaseField(columnName = TIER_COLUMN) private int tier;

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
