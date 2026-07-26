package com.eternalcode.minions.minion.storage;

import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionContext;
import java.util.Collection;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class MinionItemTransferService {

    public MinionStorageUpdate store(
            MinionContext context,
            MinionStorage storage,
            ItemStack item
    ) {
        if (context == null || storage == null || item == null) {
            throw new IllegalArgumentException("Context, storage and item are required");
        }

        ItemStack remaining = item.clone();
        Container chest = context.linkedChest();
        if (chest != null) {
            remaining = this.addToInventory(chest.getInventory(), remaining);
        }
        if (remaining == null) {
            return new MinionStorageUpdate(storage, null);
        }

        return storage.add(remaining);
    }

    public Minion deposit(
            MinionContext context,
            Minion minion,
            Location overflowLocation,
            Collection<ItemStack> items
    ) {
        if (context == null || minion == null || overflowLocation == null || items == null) {
            throw new IllegalArgumentException("Deposit context, minion, location and items are required");
        }

        MinionStorage storage = minion.storage();
        for (ItemStack item : items) {
            if (item == null || item.getType().isAir() || item.getAmount() <= 0) {
                continue;
            }

            MinionStorageUpdate update = this.store(context, storage, item);
            storage = update.storage();
            ItemStack remaining = update.remaining();
            if (remaining != null) {
                context.world().dropItemNaturally(overflowLocation, remaining);
            }
        }

        return minion.withStorage(storage);
    }

    public ItemStack addToInventory(Inventory inventory, ItemStack item) {
        if (inventory == null || item == null) {
            throw new IllegalArgumentException("Inventory and item are required");
        }

        Map<Integer, ItemStack> leftovers = inventory.addItem(item.clone());
        return leftovers.isEmpty() ? null : leftovers.get(0);
    }

    public void giveOrDrop(Player player, ItemStack item) {
        if (player == null || item == null) {
            throw new IllegalArgumentException("Player and item are required");
        }

        ItemStack remaining = this.addToInventory(player.getInventory(), item);
        if (remaining != null) {
            player.getWorld().dropItemNaturally(player.getLocation(), remaining);
        }
    }
}
