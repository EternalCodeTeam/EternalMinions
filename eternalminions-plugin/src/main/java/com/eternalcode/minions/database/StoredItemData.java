package com.eternalcode.minions.database;

public record StoredItemData(int slot, byte[] serializedItem) {

    public StoredItemData {
        if (slot < 0) {
            throw new IllegalArgumentException("Storage slot cannot be negative");
        }
        serializedItem = serializedItem.clone();
    }

    @Override
    public byte[] serializedItem() {
        return this.serializedItem.clone();
    }
}
