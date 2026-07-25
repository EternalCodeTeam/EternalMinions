package com.eternalcode.minions.minion.seller;

import com.eternalcode.minions.database.MinionPersistenceService;
import com.eternalcode.minions.minion.AbstractMinionBehavior;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionRegistry;
import com.eternalcode.minions.minion.MinionStorage;
import com.eternalcode.minions.minion.MinionType;
import com.eternalcode.minions.minion.ScheduledMinion;
import com.eternalcode.minions.minion.status.MinionStatusTracker;
import com.eternalcode.minions.render.MinionRenderer;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

// Sells configured items from the linked chest and internal storage through a shop integration.
public final class SellerBehavior extends AbstractMinionBehavior {

    private final ShopIntegration shop;

    public SellerBehavior(
        MinionRegistry registry,
        MinionPersistenceService persistence,
        MinionRenderer renderer,
        MinionStatusTracker statuses,
        ShopIntegration shop
    ) {
        super(registry, persistence, renderer, statuses);
        this.shop = shop;
    }

    @Override
    public boolean execute(Minion minion, MinionType type, ScheduledMinion scheduledMinion, World world) {
        if (!this.shop.available()) {
            this.refreshStatusIfChanged(minion, SellerStatuses.SHOP_NOT_LINKED);
            return false;
        }

        Sale sale = new Sale(type.work().sellBatch());
        Container chest = this.resolveLinkedChest(minion, world);
        if (chest != null) {
            this.sellFromInventory(type, chest.getInventory(), sale);
        }
        MinionStorage storage = this.sellFromStorage(type, minion.storage(), sale);
        if (sale.soldCount == 0) {
            this.refreshStatusIfChanged(minion, sale.sawItem ? SellerStatuses.ITEM_HAS_NO_PRICE : SellerStatuses.STORAGE_EMPTY);
            return false;
        }

        this.shop.payout(minion.ownerId(), sale.earned);
        this.commit(minion, type, storage, Float.NaN);
        this.refreshStatusIfChanged(minion, SellerStatuses.SELLING);
        return true;
    }

    private void sellFromInventory(MinionType type, Inventory inventory, Sale sale) {
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length && sale.hasBudget(); slot++) {
            ItemStack item = contents[slot];
            if (item == null) {
                continue;
            }
            sale.sawItem = true;
            double price = this.shop.priceOf(item.getType());
            if (price <= 0.0D) {
                continue;
            }

            int sold = sale.take(item.getAmount(), price);
            int left = item.getAmount() - sold;
            if (left <= 0) {
                inventory.setItem(slot, null);
            }
            else {
                item.setAmount(left);
            }
        }
    }

    private MinionStorage sellFromStorage(MinionType type, MinionStorage storage, Sale sale) {
        for (int slot = 0; slot < storage.capacity() && sale.hasBudget(); slot++) {
            ItemStack item = storage.item(slot);
            if (item == null) {
                continue;
            }
            sale.sawItem = true;
            double price = this.shop.priceOf(item.getType());
            if (price <= 0.0D) {
                continue;
            }

            int sold = sale.take(item.getAmount(), price);
            int left = item.getAmount() - sold;
            if (left <= 0) {
                storage = storage.withItem(slot, null);
            }
            else {
                ItemStack reduced = item.clone();
                reduced.setAmount(left);
                storage = storage.withItem(slot, reduced);
            }
        }
        return storage;
    }

    private static final class Sale {

        private final int batch;
        private int soldCount;
        private double earned;
        private boolean sawItem;

        private Sale(int batch) {
            this.batch = batch;
        }

        private boolean hasBudget() {
            return this.soldCount < this.batch;
        }

        private int take(int available, double price) {
            int sold = Math.min(available, this.batch - this.soldCount);
            this.soldCount += sold;
            this.earned += price * sold;
            return sold;
        }
    }
}
