package com.eternalcode.minions.minion.impl.farmer;

import com.cryptomorin.xseries.XMaterial;
import com.eternalcode.minions.config.AbstractMinionConfig;
import com.eternalcode.minions.minion.status.MinionStatus;
import com.eternalcode.minions.minion.upgrade.MinionUpgrades;
import com.eternalcode.minions.config.MinionUpgradeTierConfig;
import com.eternalcode.minions.minion.upgrade.CoreUpgradeKinds;
import com.eternalcode.minions.minion.tool.ToolCategory;
import eu.okaeri.configs.annotation.Comment;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Color;

public final class FarmerConfig extends AbstractMinionConfig {

    public FarmerConfig() {
        this.displayName = "<green>Rolnik";
        this.tool.category = ToolCategory.HOE;
        this.tool.required = true;
        this.items.helmet.texture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDAxZTAzNWEzZDhkNjEyNjA3MmJjYmU1MmE5NzkxM2FjZTkzNTUyYTk5OTk1YjVkNDA3MGQ2NzgzYTMxZTkwOSJ9fX0=";
        this.npcSkin = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHBzOi8vcy5uYW1lbWMuY29tL2kvOThkYmNkYmZiYmE3OTQ5My5wbmcifX19";
        this.items.chestplate.color = Color.fromRGB(0, 255, 0);
        this.items.leggings.color = Color.fromRGB(0, 255, 0);
        this.items.boots.color = Color.fromRGB(0, 255, 0);
        this.statuses = defaultStatuses();
    }

    @Comment("Crop block materials this minion harvests when mature.")
    public List<XMaterial> cropMaterials = List.of(XMaterial.WHEAT);

    @Comment("Seed item required to replant each crop after harvest.")
    public Map<XMaterial, XMaterial> seedByCrop = defaultSeedByCrop();

    private static Map<XMaterial, XMaterial> defaultSeedByCrop() {
        Map<XMaterial, XMaterial> seedByCrop = new LinkedHashMap<>();
        seedByCrop.put(XMaterial.WHEAT, XMaterial.WHEAT_SEEDS);
        return seedByCrop;
    }

    private static Map<MinionStatus, String> defaultStatuses() {
        Map<MinionStatus, String> statuses = new LinkedHashMap<>();
        statuses.put(FarmerStatuses.HARVESTING, "<green>Zbieranie plonów...");
        statuses.put(FarmerStatuses.NO_MATURE_CROPS, "<yellow>Brak dojrzałych upraw");
        statuses.put(FarmerStatuses.NO_SEEDS, "<red>Brak nasion");
        statuses.put(FarmerStatuses.NO_HOE, "<red>Brak motyki");
        return statuses;
    }

    public int stationCount(MinionUpgrades upgrades) {
        int purchasedTier = upgrades.tier(CoreUpgradeKinds.RANGE);
        List<MinionUpgradeTierConfig> rangeTiers = this.upgrades.get(CoreUpgradeKinds.RANGE);
        if (purchasedTier < 1 || rangeTiers == null || rangeTiers.isEmpty()) {
            return 1;
        }
        return rangeTiers.get(Math.min(purchasedTier, rangeTiers.size()) - 1).value;
    }
}
