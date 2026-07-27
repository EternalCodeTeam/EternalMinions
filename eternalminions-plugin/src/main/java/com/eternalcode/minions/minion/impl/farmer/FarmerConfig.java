package com.eternalcode.minions.minion.impl.farmer;

import com.cryptomorin.xseries.XMaterial;
import com.eternalcode.minions.config.AbstractMinionConfig;
import com.eternalcode.minions.minion.status.MinionStatus;
import com.eternalcode.minions.minion.tool.ToolCategory;
import com.eternalcode.minions.minion.upgrade.CoreUpgradeKinds;
import com.eternalcode.minions.minion.upgrade.MinionUpgrades;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Include;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Color;

@Include(AbstractMinionConfig.class)
public final class FarmerConfig extends AbstractMinionConfig {

    @Comment({
            "Defines how the farmer searches for crops:",
            "AREA - scans an area around the minion.",
            "LINE - scans a straight line in front of the minion."
    })
    public WorkMode workMode = WorkMode.AREA;
    @Comment({
            "Defines the farmer's base working range.",
            "A value of 1 creates a 3x3 area in AREA mode.",
            "In LINE mode, it means one block in front of the minion."
    })
    public int baseRange = 1;
    @Comment("Vertical crop offset relative to the minion's position.")
    public int cropYOffset = 0;
    @Comment("Supported crop materials and their harvesting behavior.")
    public Map<XMaterial, CropShape> crops = defaultCrops();
    @Comment({
            "Items consumed when replanting harvested crops.",
            "A missing entry means that the crop does not require replanting."
    })
    public Map<XMaterial, XMaterial> seeds = defaultSeeds();

    public FarmerConfig() {
        this.displayName = "<color:#4CDD0A:#ACFF87:#4CDD0A>ꜰᴀʀᴍᴇʀ";

        this.tool.category = ToolCategory.HOE;
        this.tool.required = true;

        this.items.helmet.texture =
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDAxZTAzNWEzZDhkNjEyNjA3MmJjYmU1MmE5NzkxM2FjZTkzNTUyYTk5OTk1YjVkNDA3MGQ2NzgzYTMxZTkwOSJ9fX0=";

        this.npcSkin =
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHBzOi8vcy5uYW1lbWMuY29tL2kvOThkYmNkYmZiYmE3OTQ5My5wbmcifX19";

        this.items.setLeatherArmorColor(Color.fromRGB(76, 221, 10));

        this.statuses = defaultStatuses();
        this.usageInstructions.lore = List.of(
            "<gray>1. Przygotuj dojrzałe uprawy wokół miniona.",
            "<gray>2. Włóż motykę do slotu narzędzia.",
            "<gray>3. Zapewnij nasiona do ponownego sadzenia.",
            "<gray>4. Odbieraj plony z magazynu lub podpiętej skrzyni."
        );
    }

    private static Map<XMaterial, CropShape> defaultCrops() {
        Map<XMaterial, CropShape> crops = new LinkedHashMap<>();

        crops.put(XMaterial.WHEAT, CropShape.AGEABLE_REPLANT);
        crops.put(XMaterial.CARROTS, CropShape.AGEABLE_REPLANT);
        crops.put(XMaterial.POTATOES, CropShape.AGEABLE_REPLANT);
        crops.put(XMaterial.BEETROOTS, CropShape.AGEABLE_REPLANT);
        crops.put(XMaterial.NETHER_WART, CropShape.AGEABLE_REPLANT);
        crops.put(XMaterial.COCOA, CropShape.AGEABLE_REPLANT);

        crops.put(XMaterial.SUGAR_CANE, CropShape.STACKING_COLUMN);
        crops.put(XMaterial.BAMBOO, CropShape.STACKING_COLUMN);

        crops.put(XMaterial.PUMPKIN, CropShape.STEM_FRUIT);
        crops.put(XMaterial.MELON, CropShape.STEM_FRUIT);

        return crops;
    }

    private static Map<XMaterial, XMaterial> defaultSeeds() {
        Map<XMaterial, XMaterial> seeds = new LinkedHashMap<>();

        seeds.put(XMaterial.WHEAT, XMaterial.WHEAT_SEEDS);
        seeds.put(XMaterial.CARROTS, XMaterial.CARROT);
        seeds.put(XMaterial.POTATOES, XMaterial.POTATO);
        seeds.put(XMaterial.BEETROOTS, XMaterial.BEETROOT_SEEDS);
        seeds.put(XMaterial.NETHER_WART, XMaterial.NETHER_WART);
        seeds.put(XMaterial.COCOA, XMaterial.COCOA_BEANS);

        return seeds;
    }

    private static Map<MinionStatus, String> defaultStatuses() {
        Map<MinionStatus, String> statuses = new LinkedHashMap<>();

        statuses.put(
                FarmerStatuses.HARVESTING,
                "<green>Zbieranie plonów..."
        );

        statuses.put(
                FarmerStatuses.NO_MATURE_CROPS,
                "<yellow>Brak dojrzałych upraw"
        );

        statuses.put(
                FarmerStatuses.NO_SEEDS,
                "<red>Brak nasion do ponownego zasadzenia"
        );

        statuses.put(
                FarmerStatuses.NO_HOE,
                "<red>Brak motyki"
        );

        statuses.put(
                FarmerStatuses.NO_CONFIGURED_CROPS,
                "<red>Brak poprawnie skonfigurowanych upraw"
        );

        return statuses;
    }

    public int range(MinionUpgrades upgrades) {
        int baseRange = Math.max(1, this.baseRange);
        return this.upgradeTierValueOrHigher(upgrades, CoreUpgradeKinds.RANGE, baseRange);
    }

    public enum WorkMode {

        AREA,
        LINE
    }
}
