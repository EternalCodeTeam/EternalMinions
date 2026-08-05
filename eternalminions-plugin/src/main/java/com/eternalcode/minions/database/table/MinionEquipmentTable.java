package com.eternalcode.minions.database.table;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "eternal_minion_equipment")
public final class MinionEquipmentTable {

    public static final String MINION_ID_COLUMN = "minion_id";
    public static final String SLOT_COLUMN = "slot";
    public static final String ITEM_COLUMN = "serialized_item";

    @DatabaseField(columnName = MINION_ID_COLUMN, uniqueCombo = true) private long minionId;
    @DatabaseField(columnName = SLOT_COLUMN, canBeNull = false, uniqueCombo = true) private String slot;
    @DatabaseField(columnName = ITEM_COLUMN, canBeNull = false, dataType = DataType.BYTE_ARRAY)
    private byte[] serializedItem;

    public MinionEquipmentTable() {
    }

    public MinionEquipmentTable(long minionId, String slot, byte[] serializedItem) {
        this.minionId = minionId;
        this.slot = slot;
        this.serializedItem = serializedItem.clone();
    }

    public long minionId() {return this.minionId;}

    public String slot() {return this.slot;}

    public byte[] serializedItem() {return this.serializedItem.clone();}
}
