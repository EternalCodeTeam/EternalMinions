package com.eternalcode.minions.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "eternal_minion_state")
final class MinionStateTable {

    @DatabaseField(columnName = "minion_id", id = true) private long minionId;
    @DatabaseField(columnName = "active") private boolean active;
    @DatabaseField(columnName = "level") private int level;
    @DatabaseField(columnName = "progress") private long progress;
    @DatabaseField(columnName = "updated_at") private long updatedAt;

    MinionStateTable() {
    }

    MinionStateTable(long minionId, boolean active, int level, long progress, long updatedAt) {
        this.minionId = minionId;
        this.active = active;
        this.level = level;
        this.progress = progress;
        this.updatedAt = updatedAt;
    }

    long minionId() {return this.minionId;}

    boolean active() {return this.active;}

    int level() {return this.level;}

    long progress() {return this.progress;}

    long updatedAt() {return this.updatedAt;}
}
