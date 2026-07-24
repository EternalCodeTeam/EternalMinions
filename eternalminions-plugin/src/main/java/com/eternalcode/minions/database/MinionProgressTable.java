package com.eternalcode.minions.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "eternal_minion_progress")
final class MinionProgressTable {

    @DatabaseField(columnName = "minion_id", id = true) private long minionId;
    @DatabaseField(columnName = "level") private int level;
    @DatabaseField(columnName = "progress") private long progress;

    MinionProgressTable() {
    }

    MinionProgressTable(long minionId, int level, long progress) {
        this.minionId = minionId;
        this.level = level;
        this.progress = progress;
    }

    long minionId() { return this.minionId; }
    int level() { return this.level; }
    long progress() { return this.progress; }
}
