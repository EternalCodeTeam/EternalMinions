package com.eternalcode.minions.database;

import java.util.List;

public record MinionActionUpdate(
        long minionId,
        StateChange state,
        List<EquipmentChange> equipment,
        List<StorageChange> storage
) {

    public MinionActionUpdate {
        if (minionId < 1L) {
            throw new IllegalArgumentException("Minion id must be positive");
        }
        equipment = List.copyOf(equipment);
        storage = List.copyOf(storage);
    }

    public boolean isEmpty() {
        return this.state == null && this.equipment.isEmpty() && this.storage.isEmpty();
    }

    public record StateChange(int level, long progress, long updatedAt) {

        public StateChange {
            if (level < 1 || progress < 0L || updatedAt < 0L) {
                throw new IllegalArgumentException("Invalid minion state change");
            }
        }
    }

    public record EquipmentChange(MinionEquipmentSlot slot, byte[] serializedItem) {

        public EquipmentChange {
            if (slot == null || serializedItem == null) {
                throw new IllegalArgumentException("Equipment slot and serialized item are required");
            }
            serializedItem = serializedItem.clone();
        }

        @Override
        public byte[] serializedItem() {
            return this.serializedItem.clone();
        }
    }

    public record StorageChange(int slot, byte[] serializedItem) {

        public StorageChange {
            if (slot < 0 || serializedItem == null) {
                throw new IllegalArgumentException("Storage slot and serialized item are required");
            }
            serializedItem = serializedItem.clone();
        }

        @Override
        public byte[] serializedItem() {
            return this.serializedItem.clone();
        }
    }
}
