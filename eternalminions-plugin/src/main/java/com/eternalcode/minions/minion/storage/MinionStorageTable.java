package com.eternalcode.minions.minion.storage;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "eternal_minion_storage")
public final class MinionStorageTable {

    public static final String MINION_ID_COLUMN = "minion_id";
    public static final String SLOT_COLUMN = "slot";
    public static final String ITEM_COLUMN = "serialized_item";

    @DatabaseField(columnName = MINION_ID_COLUMN, uniqueCombo = true) private long minionId;
    @DatabaseField(columnName = SLOT_COLUMN, uniqueCombo = true) private int slot;
    @DatabaseField(columnName = ITEM_COLUMN, canBeNull = false, dataType = DataType.BYTE_ARRAY)
    private byte[] serializedItem;

    public MinionStorageTable() {
    }

    public MinionStorageTable(long minionId, int slot, byte[] serializedItem) {
        this.minionId = minionId;
        this.slot = slot;
        this.serializedItem = serializedItem.clone();
    }

    public long minionId() {return this.minionId;}

    public int slot() {return this.slot;}

    public byte[] serializedItem() {return this.serializedItem.clone();}
}
