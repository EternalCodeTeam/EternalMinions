package com.eternalcode.minions.database;

import org.bukkit.inventory.ItemStack;

final class ItemDataCodec {

    private ItemDataCodec() {
    }

    static byte[] encode(ItemStack item) {
        return item == null ? new byte[0] : item.serializeAsBytes();
    }

    static ItemStack decode(byte[] serializedItem) {
        return serializedItem == null || serializedItem.length == 0
                ? null
                : ItemStack.deserializeBytes(serializedItem);
    }
}
