package com.eternalcode.minions.minion.storage;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "eternal_minion_settings")
public final class MinionSettingsTable {

    @DatabaseField(columnName = "minion_id", id = true) private long minionId;
    @DatabaseField(columnName = "direction", canBeNull = false) private String direction;

    public MinionSettingsTable() {
    }

    public MinionSettingsTable(long minionId, String direction) {
        this.minionId = minionId;
        this.direction = direction;
    }

    public long minionId() {return this.minionId;}

    public String direction() {return this.direction;}

}
