package com.eternalcode.minions.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "eternal_minions")
final class MinionTable {

    private static final String POSITION_INDEX = "eternal_minions_position_idx";

    @DatabaseField(columnName = "id", id = true) private long id;
    @DatabaseField(columnName = "owner_id", canBeNull = false, index = true) private String ownerId;
    @DatabaseField(columnName = "behavior_id", canBeNull = false) private String behaviorId;
    @DatabaseField(columnName = "world_key", canBeNull = false, indexName = POSITION_INDEX)
    private String worldKey;
    @DatabaseField(columnName = "block_x", indexName = POSITION_INDEX) private int blockX;
    @DatabaseField(columnName = "block_y") private int blockY;
    @DatabaseField(columnName = "block_z", indexName = POSITION_INDEX) private int blockZ;
    @DatabaseField(columnName = "created_at") private long createdAt;

    MinionTable() {
    }

    MinionTable(MinionData minion) {
        this.id = minion.id();
        this.ownerId = minion.ownerId().toString();
        this.behaviorId = minion.behaviorId();
        this.worldKey = minion.worldKey();
        this.blockX = minion.blockX();
        this.blockY = minion.blockY();
        this.blockZ = minion.blockZ();
        this.createdAt = minion.createdAt();
    }

    long id() {return this.id;}

    String ownerId() {return this.ownerId;}

    String behaviorId() {return this.behaviorId;}

    String worldKey() {return this.worldKey;}

    int blockX() {return this.blockX;}

    int blockY() {return this.blockY;}

    int blockZ() {return this.blockZ;}

    long createdAt() {return this.createdAt;}
}
