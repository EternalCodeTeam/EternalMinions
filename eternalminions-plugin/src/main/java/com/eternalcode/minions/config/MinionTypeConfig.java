package com.eternalcode.minions.config;

import com.cryptomorin.xseries.XMaterial;
import com.eternalcode.minions.minion.MinionBehaviorType;
import com.eternalcode.minions.minion.MinionUpgradeKind;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MinionTypeConfig extends OkaeriConfig {

    @Comment("Display name used on the minion item and hologram. Supports MiniMessage.")
    public String displayName = "<green>Górnik";

    @Comment({
        "Profession executed by this minion type.",
        "MINER breaks real blocks around itself; LUMBERJACK/FARMER/FISHERMAN/KILLER/CRAFTER generate",
        "their drop table on a timer (Hypixel-style); COLLECTOR picks up ground items; SELLER sells storage for coins."
    })
    public MinionBehaviorType behavior = MinionBehaviorType.MINER;

    @Comment("Ticks between work cycles while the minion keeps finding work.")
    public int workIntervalTicks = 40;

    @Comment("Ticks between checks while the minion has nothing to do.")
    public int idleIntervalTicks = 100;

    @Comment("Internal storage capacity in slots (1-54).")
    public int storageCapacity = 9;

    @Comment("Body scale used by the NPC renderer (0.05-10).")
    public double npcScale = 0.55;

    @Comment({
        "Base64 head texture used for the minion item, the armor stand head and the NPC skin.",
        "Paste the 'Value' field of a head from minecraft-heads.com. Empty = default head/skin."
    })
    public String headTexture = "";

    @Comment("Armor worn by the minion. Use AIR to skip a piece. PLAYER_HEAD helmet uses head-texture.")
    public XMaterial helmet = XMaterial.PLAYER_HEAD;
    public XMaterial chestplate = XMaterial.LEATHER_CHESTPLATE;
    public XMaterial leggings = XMaterial.LEATHER_LEGGINGS;
    public XMaterial boots = XMaterial.LEATHER_BOOTS;

    @Comment("Dye color applied to leather armor pieces, #RRGGBB format.")
    public String armorColor = "#D63A3A";

    @Comment({
        "Cumulative progress (finished actions) required to reach each next level.",
        "First entry unlocks level 2, second level 3, and so on. Empty list = level stays at 1."
    })
    public List<Long> levelThresholds = List.of(1_000L, 5_000L, 15_000L, 40_000L);

    @Comment({
        "Materials this minion must never break. BEDROCK is always blocked.",
        "Containers are blocked by default so the minion cannot destroy chest setups."
    })
    public List<XMaterial> blockedMaterials = List.of(
        XMaterial.SPAWNER,
        XMaterial.CHEST,
        XMaterial.TRAPPED_CHEST,
        XMaterial.BARREL,
        XMaterial.ENDER_CHEST,
        XMaterial.SHULKER_BOX
    );

    @Comment("If not empty, the minion mines ONLY these materials (whitelist).")
    public List<XMaterial> allowedMaterials = List.of();

    @Comment({
        "Drop table for generator professions (LUMBERJACK/FARMER/FISHERMAN/KILLER/CRAFTER).",
        "Each finished action rolls the whole table; chance is 0-1. Ignored by MINER/COLLECTOR/SELLER."
    })
    public List<MinionDropConfig> drops = List.of();

    @Comment("FISHERMAN only: require a water block within one block of the minion to work.")
    public boolean requiresWater = false;

    @Comment("COLLECTOR only: radius in blocks scanned for dropped items.")
    public int collectorRadiusBlocks = 4;

    @Comment({
        "SELLER only: sell price per item. The seller sells matching items from its storage",
        "(and linked chest) and pays the owner through Vault. Requires a Vault economy plugin."
    })
    public Map<XMaterial, Double> sellPrices = Map.of();

    @Comment("SELLER only: maximum number of items sold in a single action.")
    public int sellBatch = 64;

    @Comment({
        "Purchasable upgrades. Each tier: requiredLevel (minion level), value, costMaterial, costAmount.",
        "Value meaning: SPEED = work interval in ticks, RANGE = mining radius (1-3), CAPACITY = storage slots (max 54)."
    })
    public Map<MinionUpgradeKind, List<MinionUpgradeTierConfig>> upgrades = defaultUpgrades();

    private static Map<MinionUpgradeKind, List<MinionUpgradeTierConfig>> defaultUpgrades() {
        Map<MinionUpgradeKind, List<MinionUpgradeTierConfig>> upgrades = new LinkedHashMap<>();
        upgrades.put(MinionUpgradeKind.SPEED, List.of(
            new MinionUpgradeTierConfig(2, 30, XMaterial.DIAMOND, 8),
            new MinionUpgradeTierConfig(3, 20, XMaterial.DIAMOND, 16)
        ));
        upgrades.put(MinionUpgradeKind.RANGE, List.of(
            new MinionUpgradeTierConfig(2, 2, XMaterial.DIAMOND, 16),
            new MinionUpgradeTierConfig(4, 3, XMaterial.DIAMOND, 32)
        ));
        upgrades.put(MinionUpgradeKind.CAPACITY, List.of(
            new MinionUpgradeTierConfig(3, 18, XMaterial.DIAMOND, 8),
            new MinionUpgradeTierConfig(4, 27, XMaterial.DIAMOND, 16)
        ));
        return upgrades;
    }
}
