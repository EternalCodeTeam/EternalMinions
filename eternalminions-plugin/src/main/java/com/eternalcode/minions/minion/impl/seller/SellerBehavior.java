package com.eternalcode.minions.minion.impl.seller;

import com.eternalcode.minions.config.AbstractMinionConfig;
import com.eternalcode.minions.config.ConfigService;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionBehavior;
import com.eternalcode.minions.minion.MinionContext;
import com.eternalcode.minions.minion.MinionResult;
import com.eternalcode.minions.minion.WorkLimit;
import com.eternalcode.minions.minion.storage.MinionStorage;
import com.eternalcode.minions.shop.MinionShopProvider;
import java.io.File;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class SellerBehavior implements MinionBehavior {

    private final SellerConfig config;
    private final MinionShopProvider shop;

    public static SellerBehavior create(ConfigService configs, File directory, MinionShopProvider shop) {
        SellerConfig config = configs.load(SellerConfig.class, new File(directory, "seller.yml"));
        return new SellerBehavior(config, shop);
    }

    public SellerBehavior(SellerConfig config, MinionShopProvider shop) {
        this.config = config;
        this.shop = shop;
        WorkLimit.validate("seller.sellBatch", config.sellBatch);
    }

    @Override
    public String id() {
        return "seller";
    }

    @Override
    public AbstractMinionConfig config() {
        return this.config;
    }

    @Override
    public MinionResult execute(MinionContext context) {
        Minion minion = context.minion();
        if (!this.shop.available()) {
            return MinionResult.idle(minion, SellerStatuses.SHOP_NOT_LINKED);
        }

        Sale sale = new Sale(this.config.sellBatch);
        Container chest = context.linkedChest();
        Inventory linkedInventory = null;
        ItemStack[] linkedContents = null;
        if (chest != null) {
            linkedInventory = chest.getInventory();
            linkedContents = this.sellFromContents(linkedInventory.getContents(), sale);
        }
        MinionStorage storage = this.sellFromStorage(minion.storage(), sale);
        if (sale.soldCount == 0) {
            return MinionResult.idle(
                minion,
                sale.sawItem ? SellerStatuses.ITEM_HAS_NO_PRICE : SellerStatuses.STORAGE_EMPTY
            );
        }

        if (!this.shop.tryPayout(minion.ownerId(), sale.earned)) {
            return MinionResult.idle(minion, SellerStatuses.PAYOUT_FAILED);
        }
        if (linkedInventory != null) {
            linkedInventory.setContents(linkedContents);
        }
        Minion updated = minion.withStorage(storage);
        for (int soldItem = 0; soldItem < sale.soldCount; soldItem++) {
            updated = updated.withProgress(updated.progress().advanced(this.config));
        }
        return MinionResult.worked(updated, SellerStatuses.SELLING);
    }

    private ItemStack[] sellFromContents(ItemStack[] source, Sale sale) {
        ItemStack[] contents = new ItemStack[source.length];
        for (int slot = 0; slot < source.length; slot++) {
            ItemStack sourceItem = source[slot];
            contents[slot] = sourceItem == null ? null : sourceItem.clone();
        }
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

            int soldAmount = sale.take(item.getAmount(), price);
            int remainingAmount = item.getAmount() - soldAmount;
            if (remainingAmount <= 0) {
                contents[slot] = null;
            }
            else {
                item.setAmount(remainingAmount);
            }
        }
        return contents;
    }

    private MinionStorage sellFromStorage(MinionStorage storage, Sale sale) {
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

            int soldAmount = sale.take(item.getAmount(), price);
            int remainingAmount = item.getAmount() - soldAmount;
            if (remainingAmount <= 0) {
                storage = storage.withItem(slot, null);
            }
            else {
                ItemStack reduced = item.clone();
                reduced.setAmount(remainingAmount);
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
            return this.batch == 0 || this.soldCount < this.batch;
        }

        private int take(int available, double price) {
            int soldAmount = this.batch == 0
                    ? available
                    : Math.min(available, this.batch - this.soldCount);
            this.soldCount += soldAmount;
            this.earned += price * soldAmount;
            return soldAmount;
        }
    }

}
