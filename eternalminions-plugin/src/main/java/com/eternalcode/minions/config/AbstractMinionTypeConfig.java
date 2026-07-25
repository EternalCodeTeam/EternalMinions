package com.eternalcode.minions.config;

import com.cryptomorin.xseries.XMaterial;
import com.eternalcode.minions.minion.MinionUpgradeKind;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Common settings every minion profession config shares. Each profession's config class (e.g.
// FarmerConfig, MinerConfig) extends this and adds only the fields specific to it.
public abstract class AbstractMinionTypeConfig extends OkaeriConfig {

    @Comment("Display name used on the minion item and hologram. Supports MiniMessage.")
    public String displayName = "<green>Minion";

    @Comment("Ticks between work cycles while the minion keeps finding work.")
    public int workIntervalTicks = 40;

    @Comment("Ticks between checks while the minion has nothing to do.")
    public int idleIntervalTicks = 100;

    @Comment("Internal storage capacity in slots (1-54).")
    public int storageCapacity = 9;

    @Comment("Body scale used by the NPC renderer (0.05-10).")
    public double npcScale = 0.55;

    @Comment({
        "Base64 skin texture used for the NPC renderer's body (only relevant when config.yml sets",
        "minionRenderer: NPC). Independent from the helmet's own texture below. Empty = default skin."
    })
    public String npcSkin = "";

    @Comment("Armor worn by the minion, one entry per slot. Use type: AIR to skip a piece.")
    public MinionItemsConfig items = new MinionItemsConfig();

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

    @Comment("If not empty, the minion only interacts with these materials (whitelist).")
    public List<XMaterial> allowedMaterials = List.of();

    @Comment({
        "When true, the equipped tool's Efficiency level shortens the minion's action interval",
        "(each level cuts it by ~25%, floor 10% of the base interval). When false, Efficiency has",
        "no effect on speed. Fortune/Silk Touch/Unbreaking are always respected regardless."
    })
    public boolean respectSpeedEnchants = true;

    @Comment("Tool this minion type requires before it can perform profession work.")
    public MinionToolConfig tool = new MinionToolConfig();

    @Comment({
        "Purchasable upgrades. Each tier: requiredLevel (minion level), value, costMaterial, costAmount.",
        "Value meaning: SPEED = work interval in ticks, RANGE = radius (1-3), CAPACITY = storage slots (max 54)."
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
