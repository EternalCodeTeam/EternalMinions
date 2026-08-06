package com.eternalcode.minions.minion.impl.seller;

import com.cryptomorin.xseries.XMaterial;
import com.eternalcode.minions.config.AbstractMinionConfig;
import com.eternalcode.minions.minion.status.MinionStatus;
import com.eternalcode.minions.minion.tool.ToolCategory;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Include;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Color;

@Include(AbstractMinionConfig.class)
public final class SellerConfig extends AbstractMinionConfig {

    @Override
    public Path resolve(Path dataDirectory) {
        return dataDirectory.resolve("minions").resolve("seller.yml");
    }

    public SellerConfig() {
        this.displayName = "<color:#DD00FF:#FF55FF:#DD00FF>ꜱᴇʟʟᴇʀ";
        this.tool.category = ToolCategory.ANY;
        this.tool.required = false;
        this.items.helmet.texture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDI0YmE3NjBhNjFkZDI1NmM1MmIzMjUxMjlmNDYwMTZhZTg5MjIzMmEwZGVhMTcxNWY5OTdmN2M0ZDYyMmJlZiJ9fX0=";
        this.npcSkin = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHBzOi8vcy5uYW1lbWMuY29tL2kvNDRmNGI1ZThlN2I2NzExMC5wbmcifX19";
        this.items.setLeatherArmorColor(Color.fromRGB(221, 0, 255));
        this.statuses = defaultStatuses();
        this.usageInstructions.lore = List.of(
            "<gray>1. Place items in the minion's storage.",
            "<gray>2. You can also link a chest containing items.",
            "<gray>3. The minion will sell items supported by the shop.",
            "<gray>4. Sale proceeds are paid to the minion's owner."
        );
    }

    @Comment({
        "Sell price per item. The seller sells matching items from its storage",
        "(and linked chest) and pays the owner through the configured shop integration."
    })
    public Map<XMaterial, Double> sellPrices = defaultSellPrices();

    @Comment("Maximum items sold per cycle. Zero sells every supported item found.")
    public int sellBatch = 64;

    private static Map<XMaterial, Double> defaultSellPrices() {
        Map<XMaterial, Double> sellPrices = new LinkedHashMap<>();
        sellPrices.put(XMaterial.COBBLESTONE, 1.0);
        sellPrices.put(XMaterial.OAK_LOG, 2.0);
        sellPrices.put(XMaterial.WHEAT, 3.0);
        sellPrices.put(XMaterial.COD, 4.0);
        sellPrices.put(XMaterial.ROTTEN_FLESH, 1.5);
        return sellPrices;
    }

    private static Map<MinionStatus, String> defaultStatuses() {
        Map<MinionStatus, String> statuses = new LinkedHashMap<>();
        statuses.put(SellerStatuses.SELLING, "<green>Selling items...");
        statuses.put(SellerStatuses.SHOP_NOT_LINKED, "<red>Shop integration unavailable");
        statuses.put(SellerStatuses.STORAGE_EMPTY, "<yellow>Storage empty");
        statuses.put(SellerStatuses.ITEM_HAS_NO_PRICE, "<red>Item has no sell price");
        statuses.put(SellerStatuses.PAYOUT_FAILED, "<red>Could not pay the minion's owner");
        return statuses;
    }
}
