package com.eternalcode.minions.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "eternal_minion_upgrades")
final class MinionUpgradeTable {

    static final String MINION_ID_COLUMN = "minion_id";

    @DatabaseField(columnName = "id", generatedId = true) private long id;
    @DatabaseField(columnName = MINION_ID_COLUMN, uniqueCombo = true, index = true) private long minionId;
    @DatabaseField(columnName = "kind", uniqueCombo = true, canBeNull = false) private String kind;
    @DatabaseField(columnName = "tier") private int tier;

    MinionUpgradeTable() {
    }

    MinionUpgradeTable(long minionId, String kind, int tier) {
        this.minionId = minionId;
        this.kind = kind;
        this.tier = tier;
    }

    long minionId() { return this.minionId; }
    String kind() { return this.kind; }
    int tier() { return this.tier; }
}
