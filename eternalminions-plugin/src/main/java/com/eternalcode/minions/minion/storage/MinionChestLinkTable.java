package com.eternalcode.minions.minion.storage;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "eternal_minion_chest_links")
public final class MinionChestLinkTable {

    @DatabaseField(columnName = "minion_id", id = true) private long minionId;
    @DatabaseField(columnName = "world_key", canBeNull = false) private String worldKey;
    @DatabaseField(columnName = "block_x") private int blockX;
    @DatabaseField(columnName = "block_y") private int blockY;
    @DatabaseField(columnName = "block_z") private int blockZ;

    public MinionChestLinkTable() {
    }

    public MinionChestLinkTable(long minionId, String worldKey, int blockX, int blockY, int blockZ) {
        this.minionId = minionId;
        this.worldKey = worldKey;
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
    }

    public long minionId() {return this.minionId;}

    public String worldKey() {return this.worldKey;}

    public int blockX() {return this.blockX;}

    public int blockY() {return this.blockY;}

    public int blockZ() {return this.blockZ;}
}
