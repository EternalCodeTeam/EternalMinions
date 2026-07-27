package com.eternalcode.minions.minion.impl.fisherman;

import com.cryptomorin.xseries.XMaterial;
import com.eternalcode.minions.config.AbstractMinionConfig;
import com.eternalcode.minions.config.MinionUpgradeTierConfig;
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
public final class FishermanConfig extends AbstractMinionConfig {

    @Comment({
            "Horizontal distance in which the fisherman searches for water.",
            "A value of 2 scans a 5x5 area around the minion."
    })
    public int waterSearchRange = 2;
    @Comment({
            "How many blocks below the minion are included in the water search.",
            "Useful when the fisherman stands one block above the water surface."
    })
    public int waterSearchDepth = 2;
    @Comment("Minimum number of connected surface water blocks.")
    public int minWaterBlocks = 9;
    @Comment("Minimum water depth required below every valid surface block.")
    public int minWaterDepth = 2;
    @Comment("Whether the block directly above the water must be empty.")
    public boolean requireOpenSurface = true;
    @Comment("Fishing duration reduction per level of Lure.")
    public int lureTicksReductionPerLevel = 20;
    @Comment("Minimum possible fishing duration after applying Lure.")
    public int minimumWaitTicks = 100;
    @Comment("Chance in percent that a completed fishing attempt catches nothing.")
    public double emptyCatchChance = 20.0;
    @Comment("Relative weight of the fish category after a successful catch.")
    public double fishCategoryWeight = 85.0;
    @Comment("Relative weight of the junk category after a successful catch.")
    public double junkCategoryWeight = 10.0;
    @Comment("Relative weight of the treasure category after a successful catch.")
    public double treasureCategoryWeight = 5.0;
    @Comment("Treasure category weight added per level of Luck of the Sea.")
    public double luckTreasureWeightPerLevel = 1.0;
    @Comment("Junk category weight removed per level of Luck of the Sea.")
    public double luckJunkWeightReductionPerLevel = 2.0;
    @Comment("Relative material weights inside the fish category.")
    public Map<XMaterial, Integer> fishLoot = defaultFishLoot();
    @Comment("Relative material weights inside the junk category.")
    public Map<XMaterial, Integer> junkLoot = defaultJunkLoot();
    @Comment("Relative material weights inside the treasure category.")
    public Map<XMaterial, Integer> treasureLoot = defaultTreasureLoot();

    public FishermanConfig() {
        this.displayName = "<color:#4498DB:#5CB6FF:#4498DB>ꜰɪꜱʜᴇʀᴍᴀɴ";
        this.workIntervalTicks = 200;

        this.tool.category = ToolCategory.FISHING_ROD;
        this.tool.required = true;
        this.upgrades.put(CoreUpgradeKinds.SPEED, List.of(
                new MinionUpgradeTierConfig(2, 170, XMaterial.DIAMOND, 8),
                new MinionUpgradeTierConfig(3, 140, XMaterial.DIAMOND, 16)
        ));

        this.items.helmet.texture =
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWMxNWU1ZmI1NmZhMTZiMDc0N2IxYmNiMDUzMzVmNTVkMWZhMzE1NjFjMDgyYjVlMzY0M2RiNTU2NTQxMDg1MiJ9fX0=";

        this.npcSkin =
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHBzOi8vcy5uYW1lbWMuY29tL2kvODRmMjljMTA5YjNiZjRiYS5wbmcifX19";

        this.items.setLeatherArmorColor(Color.fromRGB(68, 152, 219));

        this.statuses = defaultStatuses();

        this.usageInstructions.lore = List.of(
                "<gray>1. Postaw rybaka obok zbiornika wody.",
                "<gray>2. Włóż wędkę do slotu narzędzia.",
                "<gray>3. Zostaw miejsce na złowione przedmioty.",
                "",
                "<dark_gray>Wymagania zbiornika:",
                "<gray>• Minimum <white>9</white> bloków powierzchni.",
                "<gray>• Minimum <white>2</white> bloków głębokości.",
                "<gray>• Wolna przestrzeń nad powierzchnią.",
                "",
                "<dark_gray>Połów:",
                "<gray>• Jedna próba trwa bazowo <white>10 sekund</white>.",
                "<gray>• Próba może zakończyć się bez zdobyczy.",
                "",
                "<dark_gray>Zaklęcia:",
                "<aqua>• Lure <gray>skraca czas oczekiwania.",
                "<aqua>• Luck of the Sea <gray>poprawia zdobycz.",
                "<aqua>• Unbreaking <gray>zmniejsza zużycie wędki."
        );
    }

    private static Map<XMaterial, Integer> defaultFishLoot() {
        Map<XMaterial, Integer> loot = new LinkedHashMap<>();
        loot.put(XMaterial.COD, 60);
        loot.put(XMaterial.SALMON, 25);
        loot.put(XMaterial.PUFFERFISH, 13);
        loot.put(XMaterial.TROPICAL_FISH, 2);
        return loot;
    }

    private static Map<XMaterial, Integer> defaultJunkLoot() {
        Map<XMaterial, Integer> loot = new LinkedHashMap<>();
        loot.put(XMaterial.LILY_PAD, 17);
        loot.put(XMaterial.BOWL, 10);
        loot.put(XMaterial.LEATHER, 10);
        loot.put(XMaterial.LEATHER_BOOTS, 10);
        loot.put(XMaterial.ROTTEN_FLESH, 10);
        loot.put(XMaterial.STICK, 10);
        loot.put(XMaterial.STRING, 10);
        loot.put(XMaterial.BONE, 10);
        loot.put(XMaterial.INK_SAC, 10);
        loot.put(XMaterial.TRIPWIRE_HOOK, 3);
        return loot;
    }

    private static Map<XMaterial, Integer> defaultTreasureLoot() {
        Map<XMaterial, Integer> loot = new LinkedHashMap<>();
        loot.put(XMaterial.BOW, 15);
        loot.put(XMaterial.BOOK, 15);
        loot.put(XMaterial.FISHING_ROD, 15);
        loot.put(XMaterial.NAME_TAG, 20);
        loot.put(XMaterial.NAUTILUS_SHELL, 20);
        loot.put(XMaterial.SADDLE, 15);
        return loot;
    }

    private static Map<MinionStatus, String> defaultStatuses() {
        Map<MinionStatus, String> statuses =
                new LinkedHashMap<>();

        statuses.put(
                FishermanStatuses.FISHING,
                "<aqua>Łowienie..."
        );

        statuses.put(
                FishermanStatuses.CATCHING,
                "<green>Wyławianie zdobyczy..."
        );

        statuses.put(
                FishermanStatuses.NOTHING_CAUGHT,
                "<gray>Tym razem nic nie złowiono"
        );

        statuses.put(
                FishermanStatuses.NO_WATER_NEARBY,
                "<red>Brak wody w pobliżu"
        );

        statuses.put(
                FishermanStatuses.WATER_TOO_SMALL,
                "<yellow>Zbiornik jest zbyt mały"
        );

        statuses.put(
                FishermanStatuses.WATER_TOO_SHALLOW,
                "<yellow>Zbiornik jest zbyt płytki"
        );

        statuses.put(
                FishermanStatuses.WATER_SURFACE_BLOCKED,
                "<yellow>Powierzchnia wody jest zablokowana"
        );

        statuses.put(
                FishermanStatuses.NO_ROD,
                "<red>Brak wędki"
        );

        return statuses;
    }

    public int searchRange() {
        return Math.max(1, this.waterSearchRange);
    }

    public int searchDepth() {
        return Math.max(0, this.waterSearchDepth);
    }

    public int requiredWaterBlocks() {
        return Math.max(1, this.minWaterBlocks);
    }

    public int requiredWaterDepth() {
        return Math.max(1, this.minWaterDepth);
    }

    public long fishingWaitTicks(
            int lureLevel,
            MinionUpgrades minionUpgrades
    ) {
        long reduction =
                (long) Math.max(0, this.lureTicksReductionPerLevel)
                        * Math.max(0, lureLevel);
        long baseWaitTicks = Math.max(
                1L,
                this.workInterval(minionUpgrades)
        );

        return Math.max(
                Math.max(1, this.minimumWaitTicks),
                baseWaitTicks - reduction
        );
    }
}
