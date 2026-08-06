package com.eternalcode.minions.config;

import com.cryptomorin.xseries.XMaterial;
import com.eternalcode.minions.minion.upgrade.MinionUpgradeTier;
import com.eternalcode.minions.minion.upgrade.MinionUpgrades;
import com.eternalcode.minions.minion.status.MinionStatus;
import com.eternalcode.minions.minion.tool.ToolCategory;
import com.eternalcode.minions.minion.tool.ToolRequirement;
import com.eternalcode.minions.minion.upgrade.DefaultUpgradeKinds;
import com.eternalcode.minions.minion.upgrade.UpgradeKind;
import eu.okaeri.configs.annotation.Comment;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;

// Common settings every minion profession config shares. Each profession's config class (e.g.
// FarmerConfig, MinerConfig) extends this and adds only the fields specific to it.
public abstract class AbstractMinionConfig extends ConfigurationFile {

    @Comment("Whether this minion type is enabled. Set to false to disable it entirely.")
    public boolean enabled = true;

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
        "Lore displayed on the physical minion item. Supports MiniMessage.",
        "Placeholders: {MINION_LEVEL}, {MINION_MAX_LEVEL}, {MINION_PROGRESS},",
        "{MINION_PROGRESS_REQUIRED}, {STORAGE_USED}, {STORAGE_CAPACITY}."
    })
    public List<String> itemLore = defaultItemLore();

    @Comment("Text used for {MINION_PROGRESS_REQUIRED} at the maximum level.")
    public String maximumProgressText = "MAX";

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

    @Comment("Tool this minion type requires before it can perform profession work.")
    public MinionToolConfig tool = new MinionToolConfig();

    @Comment({
        "Purchasable upgrades paid through Vault. Each tier: requiredLevel, value and costAmount.",
        "Value meaning: SPEED = work interval in ticks, RANGE = radius (1-3), CAPACITY = storage slots (max 54)."
    })
    public Map<UpgradeKind, List<MinionUpgradeTierConfig>> upgrades = defaultUpgrades();

    @Comment("Status text shown in the hologram, keyed by status name. Supports MiniMessage.")
    public Map<MinionStatus, String> statuses = Map.of();

    @Comment("Instruction item displayed in the minion panel.")
    public MinionPanelElementConfig usageInstructions = defaultUsageInstructions();

    private static List<String> defaultItemLore() {
        return List.of(
            "<gray>Level: <white>{MINION_LEVEL}<gray>/<white>{MINION_MAX_LEVEL}",
            "<gray>Progress: <white>{MINION_PROGRESS}<gray>/<white>{MINION_PROGRESS_REQUIRED}",
            "<gray>Storage: <white>{STORAGE_USED}<gray>/<white>{STORAGE_CAPACITY}",
            "",
            "<dark_gray>Right-click a block to place this minion."
        );
    }

    private static MinionPanelElementConfig defaultUsageInstructions() {
        MinionPanelElementConfig instructions = new MinionPanelElementConfig();
        instructions.material = XMaterial.KNOWLEDGE_BOOK;
        instructions.displayName = "<yellow>How to Use";
        instructions.lore = List.of("<gray>No usage instructions have been configured.");
        return instructions;
    }

    private static Map<UpgradeKind, List<MinionUpgradeTierConfig>> defaultUpgrades() {
        Map<UpgradeKind, List<MinionUpgradeTierConfig>> upgrades = new LinkedHashMap<>();
        upgrades.put(
                DefaultUpgradeKinds.SPEED, List.of(
            new MinionUpgradeTierConfig(2, 30, new BigDecimal("8.00")),
            new MinionUpgradeTierConfig(3, 20, new BigDecimal("16.00"))
        ));
        upgrades.put(
                DefaultUpgradeKinds.RANGE, List.of(
            new MinionUpgradeTierConfig(2, 2, new BigDecimal("16.00")),
            new MinionUpgradeTierConfig(4, 3, new BigDecimal("32.00"))
        ));
        upgrades.put(
                DefaultUpgradeKinds.CAPACITY, List.of(
            new MinionUpgradeTierConfig(3, 18, new BigDecimal("8.00")),
            new MinionUpgradeTierConfig(4, 27, new BigDecimal("16.00"))
        ));
        return upgrades;
    }

    public long workInterval(MinionUpgrades minionUpgrades) {
        return this.upgradeTierValue(minionUpgrades, DefaultUpgradeKinds.SPEED, this.workIntervalTicks);
    }

    public int storageCapacity(MinionUpgrades minionUpgrades) {
        return this.upgradeTierValue(minionUpgrades, DefaultUpgradeKinds.CAPACITY, this.storageCapacity);
    }

    // Shared by every profession config's radius/range/cooldown-style getters: reads the tier
    // unlocked by the minion's purchased upgrade level, falling back when nothing was purchased.
    public int upgradeTierValue(MinionUpgrades minionUpgrades, UpgradeKind kind, int fallback) {
        int purchasedTier = minionUpgrades.tier(kind);
        List<MinionUpgradeTierConfig> tiers = this.upgrades.get(kind);
        if (purchasedTier < 1 || tiers == null || tiers.isEmpty()) {
            return fallback;
        }

        int tierIndex = Math.min(purchasedTier, tiers.size()) - 1;
        return tiers.get(tierIndex).value;
    }

    // Same as upgradeTierValue, but never lets the upgrade drop the value below the configured base.
    public int upgradeTierValueOrHigher(MinionUpgrades minionUpgrades, UpgradeKind kind, int baseValue) {
        return Math.max(baseValue, this.upgradeTierValue(minionUpgrades, kind, baseValue));
    }

    public int maxLevel() {
        return this.levelThresholds.size() + 1;
    }

    public long progressToReach(int level) {
        if (level < 2 || level > this.maxLevel()) {
            throw new IllegalArgumentException("Level " + level + " is outside this minion config");
        }
        return this.levelThresholds.get(level - 2);
    }

    public int maxUpgradeTier(UpgradeKind kind) {
        List<MinionUpgradeTierConfig> tiers = this.upgrades.get(kind);
        return tiers == null ? 0 : tiers.size();
    }

    public MinionUpgradeTier upgradeTier(UpgradeKind kind, int tier) {
        List<MinionUpgradeTierConfig> tiers = this.upgrades.get(kind);
        if (tiers == null || tier < 1 || tier > tiers.size()) {
            throw new IllegalArgumentException("Upgrade " + kind + " tier " + tier + " does not exist");
        }

        MinionUpgradeTierConfig configuredTier = tiers.get(tier - 1);
        return new MinionUpgradeTier(
            configuredTier.requiredLevel,
            configuredTier.value,
            configuredTier.costAmount
        );
    }

    public ToolRequirement toolRequirement() {
        ToolCategory category = this.tool.category == ToolCategory.NONE ? ToolCategory.ANY : this.tool.category;
        return new ToolRequirement(
            category,
            this.materials(this.tool.allowedMaterials),
            this.tool.minDurabilityToKeep,
            this.tool.required
        );
    }

    public Set<Material> materials(List<XMaterial> configuredMaterials) {
        Set<Material> materials = EnumSet.noneOf(Material.class);
        for (XMaterial configuredMaterial : configuredMaterials) {
            Material material = configuredMaterial.parseMaterial();
            if (material != null) {
                materials.add(material);
            }
        }
        return Set.copyOf(materials);
    }
}
