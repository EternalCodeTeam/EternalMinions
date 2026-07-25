package com.eternalcode.minions.minion.seller;

import com.cryptomorin.xseries.XMaterial;
import com.eternalcode.minions.config.AbstractMinionTypeConfig;
import com.eternalcode.minions.minion.status.MinionStatus;
import com.eternalcode.minions.minion.tool.ToolCategory;
import eu.okaeri.configs.annotation.Comment;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.Color;

public final class SellerConfig extends AbstractMinionTypeConfig {

    public SellerConfig() {
        this.displayName = "<light_purple>Sprzedawca";
        this.tool.category = ToolCategory.ANY;
        this.tool.required = false;
        this.items.helmet.texture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDI0YmE3NjBhNjFkZDI1NmM1MmIzMjUxMjlmNDYwMTZhZTg5MjIzMmEwZGVhMTcxNWY5OTdmN2M0ZDYyMmJlZiJ9fX0=";
        this.npcSkin = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHBzOi8vcy5uYW1lbWMuY29tL2kvNDRmNGI1ZThlN2I2NzExMC5wbmcifX19";
        this.items.chestplate.color = Color.fromRGB(200, 0, 255);
        this.items.leggings.color = Color.fromRGB(200, 0, 255);
        this.items.boots.color = Color.fromRGB(200, 0, 255);
    }

    @Comment({
        "Sell price per item. The seller sells matching items from its storage",
        "(and linked chest) and pays the owner through the configured shop integration."
    })
    public Map<XMaterial, Double> sellPrices = defaultSellPrices();

    @Comment("Maximum number of items sold in a single action.")
    public int sellBatch = 64;

    @Comment("Status text shown in the hologram, keyed by status name. Supports MiniMessage.")
    public Map<MinionStatus, String> statuses = defaultStatuses();

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
        statuses.put(SellerStatuses.SELLING, "<green>Sprzedawanie...");
        statuses.put(SellerStatuses.SHOP_NOT_LINKED, "<red>Sklep nie jest podpięty");
        statuses.put(SellerStatuses.STORAGE_EMPTY, "<yellow>Magazyn pusty");
        statuses.put(SellerStatuses.ITEM_HAS_NO_PRICE, "<red>Przedmiot nie ma ceny");
        return statuses;
    }
}
