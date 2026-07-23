package com.eternalcode.minions.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "eternal_minion_progress")
final class MinionProgressTable {

    @DatabaseField(columnName = "minion_id", id = true) private long minionId;
    @DatabaseField(columnName = "level") private int level;

    MinionProgressTable() {
    }

    MinionProgressTable(long minionId, int level) {
        this.minionId = minionId;
        this.level = level;
    }

    long minionId() { return this.minionId; }
    int level() { return this.level; }
}
