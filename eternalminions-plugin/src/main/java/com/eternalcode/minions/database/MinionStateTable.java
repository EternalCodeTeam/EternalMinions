package com.eternalcode.minions.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "eternal_minion_state")
final class MinionStateTable {

    @DatabaseField(columnName = "minion_id", id = true) private long minionId;
    @DatabaseField(columnName = "level") private int level;
    @DatabaseField(columnName = "progress") private long progress;
    @DatabaseField(columnName = "updated_at") private long updatedAt;

    MinionStateTable() {
    }

    MinionStateTable(long minionId, int level, long progress, long updatedAt) {
        this.minionId = minionId;
        this.level = level;
        this.progress = progress;
        this.updatedAt = updatedAt;
    }

    long minionId() {return this.minionId;}

    int level() {return this.level;}

    long progress() {return this.progress;}

    long updatedAt() {return this.updatedAt;}
}
