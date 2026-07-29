package com.eternalcode.minions.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "eternal_minion_settings")
final class MinionSettingsTable {

    @DatabaseField(columnName = "minion_id", id = true) private long minionId;
    @DatabaseField(columnName = "direction", canBeNull = false) private String direction;

    MinionSettingsTable() {
    }

    MinionSettingsTable(long minionId, String direction) {
        this.minionId = minionId;
        this.direction = direction;
    }

    long minionId() {return this.minionId;}

    String direction() {return this.direction;}

}
