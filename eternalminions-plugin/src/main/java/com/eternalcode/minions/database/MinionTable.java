package com.eternalcode.minions.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import java.util.UUID;

@DatabaseTable(tableName = "eternal_minions")
final class MinionTable {

    @DatabaseField(columnName = "id", id = true) private long id;
    @DatabaseField(columnName = "owner_id", canBeNull = false, index = true) private UUID ownerId;
    @DatabaseField(columnName = "behavior_id", canBeNull = false) private String behaviorId;
    @DatabaseField(columnName = "world_key", canBeNull = false) private String worldKey;
    @DatabaseField(columnName = "block_x") private int blockX;
    @DatabaseField(columnName = "block_y") private int blockY;
    @DatabaseField(columnName = "block_z") private int blockZ;
    @DatabaseField(columnName = "active") private boolean active;

    MinionTable() {
    }

    MinionTable(MinionData minion) {
        this.id = minion.id();
        this.ownerId = minion.ownerId();
        this.behaviorId = minion.behaviorId();
        this.worldKey = minion.worldKey();
        this.blockX = minion.blockX();
        this.blockY = minion.blockY();
        this.blockZ = minion.blockZ();
        this.active = minion.active();
    }

    long id() { return this.id; }
    UUID ownerId() { return this.ownerId; }
    String behaviorId() { return this.behaviorId; }
    String worldKey() { return this.worldKey; }
    int blockX() { return this.blockX; }
    int blockY() { return this.blockY; }
    int blockZ() { return this.blockZ; }
    boolean active() { return this.active; }
}
