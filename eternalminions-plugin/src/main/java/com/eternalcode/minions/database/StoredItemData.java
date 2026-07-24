package com.eternalcode.minions.database;

public record StoredItemData(int slot, byte[] serializedItem) {

    public StoredItemData {
        if (slot < 0) {
            throw new IllegalArgumentException("Storage slot cannot be negative");
        }
        if (serializedItem == null) {
            throw new IllegalArgumentException("Serialized storage item is required");
        }
        serializedItem = serializedItem.clone();
    }

    @Override
    public byte[] serializedItem() {
        return this.serializedItem.clone();
    }
}
