package com.eternalcode.minions.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "eternal_minion_chest_links")
final class MinionChestLinkTable {

    @DatabaseField(columnName = "minion_id", id = true) private long minionId;
    @DatabaseField(columnName = "world_key", canBeNull = false) private String worldKey;
    @DatabaseField(columnName = "block_x") private int blockX;
    @DatabaseField(columnName = "block_y") private int blockY;
    @DatabaseField(columnName = "block_z") private int blockZ;

    MinionChestLinkTable() {
    }

    MinionChestLinkTable(long minionId, String worldKey, int blockX, int blockY, int blockZ) {
        this.minionId = minionId;
        this.worldKey = worldKey;
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
    }

    long minionId() {return this.minionId;}

    String worldKey() {return this.worldKey;}

    int blockX() {return this.blockX;}

    int blockY() {return this.blockY;}

    int blockZ() {return this.blockZ;}
}
