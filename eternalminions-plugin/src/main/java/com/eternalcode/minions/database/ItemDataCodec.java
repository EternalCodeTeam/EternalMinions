package com.eternalcode.minions.database;

import org.bukkit.inventory.ItemStack;

public final class ItemDataCodec {

    private ItemDataCodec() {
    }

    public static byte[] encode(ItemStack item) {
        return item == null ? new byte[0] : item.serializeAsBytes();
    }

    public static ItemStack decode(byte[] serializedItem) {
        return serializedItem == null || serializedItem.length == 0
                ? null
                : ItemStack.deserializeBytes(serializedItem);
    }

    public static byte[] encodeInteger(int value) {
        return new byte[] {
                (byte) (value >>> 24),
                (byte) (value >>> 16),
                (byte) (value >>> 8),
                (byte) value
        };
    }

    public static int decodeInteger(byte[] encodedValue) {
        if (encodedValue == null || encodedValue.length != Integer.BYTES) {
            throw new IllegalArgumentException("Encoded integer must contain exactly four bytes");
        }

        return (encodedValue[0] & 0xFF) << 24
                | (encodedValue[1] & 0xFF) << 16
                | (encodedValue[2] & 0xFF) << 8
                | encodedValue[3] & 0xFF;
    }
}
