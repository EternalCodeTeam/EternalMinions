package com.eternalcode.minions.database;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "eternal_minion_equipment")
final class MinionEquipmentTable {

    static final String MINION_ID_COLUMN = "minion_id";

    @DatabaseField(columnName = "id", generatedId = true) private long id;
    @DatabaseField(columnName = MINION_ID_COLUMN, uniqueCombo = true, index = true) private long minionId;
    @DatabaseField(columnName = "slot", canBeNull = false, uniqueCombo = true) private String slot;
    @DatabaseField(columnName = "serialized_item", canBeNull = false, dataType = DataType.BYTE_ARRAY)
    private byte[] serializedItem;

    MinionEquipmentTable() {
    }

    MinionEquipmentTable(long minionId, String slot, byte[] serializedItem) {
        this.minionId = minionId;
        this.slot = slot;
        this.serializedItem = serializedItem.clone();
    }

    long minionId() { return this.minionId; }
    String slot() { return this.slot; }
    byte[] serializedItem() { return this.serializedItem.clone(); }
}
