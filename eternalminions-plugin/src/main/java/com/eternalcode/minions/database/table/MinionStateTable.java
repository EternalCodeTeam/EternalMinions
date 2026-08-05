package com.eternalcode.minions.database.table;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "eternal_minion_state")
public final class MinionStateTable {

    @DatabaseField(columnName = "minion_id", id = true) private long minionId;
    @DatabaseField(columnName = "level") private int level;
    @DatabaseField(columnName = "progress") private long progress;
    @DatabaseField(columnName = "updated_at") private long updatedAt;

    public MinionStateTable() {
    }

    public MinionStateTable(long minionId, int level, long progress, long updatedAt) {
        this.minionId = minionId;
        this.level = level;
        this.progress = progress;
        this.updatedAt = updatedAt;
    }

    public long minionId() {return this.minionId;}

    public int level() {return this.level;}

    public long progress() {return this.progress;}

    public long updatedAt() {return this.updatedAt;}
}
