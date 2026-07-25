package com.eternalcode.minions.minion;

import com.cryptomorin.xseries.XMaterial;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.eternalcode.minions.config.AbstractMinionTypeConfig;
import com.eternalcode.minions.config.ConfigService;
import com.eternalcode.minions.config.MinionArmorPieceConfig;
import com.eternalcode.minions.config.MinionRecipeConfig;
import com.eternalcode.minions.config.MinionUpgradeTierConfig;
import com.eternalcode.minions.config.MinionsConfig;
import com.eternalcode.minions.minion.collector.CollectorConfig;
import com.eternalcode.minions.minion.crafter.CrafterConfig;
import com.eternalcode.minions.minion.crafter.CrafterWork;
import com.eternalcode.minions.minion.crafter.MinionRecipe;
import com.eternalcode.minions.minion.farmer.FarmerConfig;
import com.eternalcode.minions.minion.farmer.FarmerWork;
import com.eternalcode.minions.minion.fisherman.FisherWork;
import com.eternalcode.minions.minion.fisherman.FishermanConfig;
import com.eternalcode.minions.minion.killer.KillerConfig;
import com.eternalcode.minions.minion.killer.KillerWork;
import com.eternalcode.minions.minion.lumberjack.LumberjackConfig;
import com.eternalcode.minions.minion.lumberjack.LumberjackWork;
import com.eternalcode.minions.minion.miner.MinerConfig;
import com.eternalcode.minions.minion.seller.SellerConfig;
import com.eternalcode.minions.minion.status.MinionStatus;
import com.eternalcode.minions.minion.tool.ToolCategory;
import com.eternalcode.minions.minion.tool.ToolRequirement;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;

// Loads one fixed, dedicated config file per profession (miner.yml, farmer.yml, ...), each backed
// by its own OkaeriConfig class in that profession's package. There is exactly one MinionType per
// profession - no generic multi-type-per-profession directory scan, kept simple on purpose.
public final class MinionTypeService {

    private final Server server;
    private final MinionsConfig minionsConfig;
    private final ConfigService configs;
    private final File directory;
    private Map<MinionBehaviorType, MinionType> types = Map.of();

    public MinionTypeService(ConfigService configs, MinionsConfig minionsConfig, Server server, File directory) {
        this.server = server;
        this.minionsConfig = minionsConfig;
        this.configs = configs;
        this.directory = directory;
        directory.mkdirs();
        this.rebuild();
    }

    public Optional<MinionType> type(String typeId) {
        for (MinionBehaviorType behavior : MinionBehaviorType.values()) {
            if (behavior.name().equalsIgnoreCase(typeId)) {
                return Optional.ofNullable(this.types.get(behavior));
            }
        }
        return Optional.empty();
    }

    public Optional<MinionType> type(MinionBehaviorType behavior) {
        return Optional.ofNullable(this.types.get(behavior));
    }

    public MinionType defaultType() {
        return this.types.get(MinionBehaviorType.MINER);
    }

    public Collection<MinionType> types() {
        return this.types.values();
    }

    public void rebuild() {
        Map<MinionBehaviorType, MinionType> rebuilt = new EnumMap<>(MinionBehaviorType.class);
        rebuilt.put(MinionBehaviorType.MINER, this.mapMiner(this.load("miner", MinerConfig.class)));
        rebuilt.put(MinionBehaviorType.LUMBERJACK, this.mapLumberjack(this.load("lumberjack", LumberjackConfig.class)));
        rebuilt.put(MinionBehaviorType.FARMER, this.mapFarmer(this.load("farmer", FarmerConfig.class)));
        rebuilt.put(MinionBehaviorType.FISHERMAN, this.mapFisherman(this.load("fisherman", FishermanConfig.class)));
        rebuilt.put(MinionBehaviorType.KILLER, this.mapKiller(this.load("killer", KillerConfig.class)));
        rebuilt.put(MinionBehaviorType.COLLECTOR, this.mapCollector(this.load("collector", CollectorConfig.class)));
        rebuilt.put(MinionBehaviorType.CRAFTER, this.mapCrafter(this.load("crafter", CrafterConfig.class)));
        rebuilt.put(MinionBehaviorType.SELLER, this.mapSeller(this.load("seller", SellerConfig.class)));
        this.types = Map.copyOf(rebuilt);
    }

    private <T extends AbstractMinionTypeConfig> T load(String fileName, Class<T> configType) {
        return this.configs.create(configType, new File(this.directory, fileName + ".yml"));
    }

    // ---- Per-profession MinionType assembly. Each builds the profession-specific MinionWork
    // sub-object from its own config fields, then delegates the shared plumbing to mapCommon. ----

    private MinionType mapMiner(MinerConfig config) {
        return this.mapCommon("miner", MinionBehaviorType.MINER, config, config.statuses, this.emptyWork(config));
    }

    private MinionType mapLumberjack(LumberjackConfig config) {
        Material logMaterial = config.logMaterial.parseMaterial();
        Material saplingMaterial = config.saplingMaterial.parseMaterial();
        if (logMaterial == null || saplingMaterial == null) {
            throw new IllegalArgumentException(
                "Lumberjack log/sapling material unavailable on this server version: "
                    + config.logMaterial + "/" + config.saplingMaterial);
        }
        LumberjackWork lumberjackWork = new LumberjackWork(logMaterial, saplingMaterial, config.maxLogsPerTree);
        MinionWork work = this.workBuilder(config)
            .lumberjack(lumberjackWork)
            .build();
        return this.mapCommon("lumberjack", MinionBehaviorType.LUMBERJACK, config, config.statuses, work);
    }

    private MinionType mapFarmer(FarmerConfig config) {
        Set<Material> cropMaterials = mapMaterials(config.cropMaterials);
        if (cropMaterials.isEmpty()) {
            cropMaterials = EnumSet.of(Material.WHEAT);
        }
        Map<Material, Material> seedByCrop = new EnumMap<>(Material.class);
        for (Map.Entry<XMaterial, XMaterial> entry : config.seedByCrop.entrySet()) {
            Material crop = entry.getKey().parseMaterial();
            Material seed = entry.getValue().parseMaterial();
            if (crop != null && seed != null) {
                seedByCrop.put(crop, seed);
            }
        }
        FarmerWork farmerWork = new FarmerWork(cropMaterials, seedByCrop);
        MinionWork work = this.workBuilder(config)
            .farmer(farmerWork)
            .build();
        return this.mapCommon("farmer", MinionBehaviorType.FARMER, config, config.statuses, work);
    }

    private MinionType mapFisherman(FishermanConfig config) {
        FisherWork fisherWork = new FisherWork(config.minWaterBlocks, config.baseWaitTicks, config.lureTicksReductionPerLevel);
        MinionWork work = this.workBuilder(config)
            .fisher(fisherWork)
            .build();
        return this.mapCommon("fisherman", MinionBehaviorType.FISHERMAN, config, config.statuses, work);
    }

    private MinionType mapKiller(KillerConfig config) {
        Set<org.bukkit.entity.EntityType> allowedMobs = config.allowedMobs.isEmpty()
            ? EnumSet.of(org.bukkit.entity.EntityType.ZOMBIE)
            : EnumSet.copyOf(config.allowedMobs);
        KillerWork killerWork = new KillerWork(allowedMobs, config.attackRangeBlocks, config.attackCooldownTicks, config.baseDamage);
        MinionWork work = this.workBuilder(config)
            .killer(killerWork)
            .build();
        return this.mapCommon("killer", MinionBehaviorType.KILLER, config, config.statuses, work);
    }

    private MinionType mapCollector(CollectorConfig config) {
        MinionWork work = this.workBuilder(config)
            .collectorRadius(config.collectorRadiusBlocks)
            .collectorAllowed(mapMaterials(config.collectorAllowedMaterials))
            .collectorBlocked(mapMaterials(config.collectorBlockedMaterials))
            .build();
        return this.mapCommon("collector", MinionBehaviorType.COLLECTOR, config, config.statuses, work);
    }

    private MinionType mapCrafter(CrafterConfig config) {
        List<MinionRecipe> recipes = new ArrayList<>();
        for (MinionRecipeConfig recipeConfig : config.recipes) {
            Map<Material, Integer> ingredients = new EnumMap<>(Material.class);
            for (Map.Entry<XMaterial, Integer> entry : recipeConfig.ingredients.entrySet()) {
                Material material = entry.getKey().parseMaterial();
                if (material != null) {
                    ingredients.put(material, entry.getValue());
                }
            }
            Material result = recipeConfig.resultMaterial.parseMaterial();
            if (result != null && !ingredients.isEmpty()) {
                recipes.add(new MinionRecipe(
                    recipeConfig.id, recipeConfig.displayName, ingredients, result, recipeConfig.resultAmount));
            }
        }
        MinionWork work = this.workBuilder(config)
            .crafter(new CrafterWork(recipes))
            .build();
        return this.mapCommon("crafter", MinionBehaviorType.CRAFTER, config, config.statuses, work);
    }

    private MinionType mapSeller(SellerConfig config) {
        Map<Material, Double> sellPrices = new EnumMap<>(Material.class);
        for (Map.Entry<XMaterial, Double> entry : config.sellPrices.entrySet()) {
            Material material = entry.getKey().parseMaterial();
            if (material != null) {
                sellPrices.put(material, entry.getValue());
            }
        }
        MinionWork work = this.workBuilder(config)
            .sellPrices(sellPrices)
            .sellBatch(config.sellBatch)
            .build();
        return this.mapCommon("seller", MinionBehaviorType.SELLER, config, config.statuses, work);
    }

    // A profession that needs none of another profession's tuning still gets a valid, unused
    // MinionWork - the builder fills every slot with a harmless default.
    private MinionWork emptyWork(AbstractMinionTypeConfig config) {
        return this.workBuilder(config).build();
    }

    private MinionWorkBuilder workBuilder(AbstractMinionTypeConfig config) {
        return new MinionWorkBuilder(this.mapToolRequirement(config));
    }

    private MinionType mapCommon(
        String typeId,
        MinionBehaviorType behavior,
        AbstractMinionTypeConfig config,
        Map<MinionStatus, String> professionStatuses,
        MinionWork work
    ) {
        ItemStack headItem = this.createHeadItem(config.items.helmet.texture);
        Map<MinionStatus, String> statusTexts = new LinkedHashMap<>(this.minionsConfig.statuses);
        statusTexts.putAll(professionStatuses);
        return new MinionType(
            typeId,
            config.displayName,
            behavior,
            config.workIntervalTicks,
            config.idleIntervalTicks,
            config.storageCapacity,
            config.npcScale,
            config.items.helmet.texture,
            config.npcSkin,
            headItem,
            this.createArmorPiece(config.items.helmet, headItem),
            this.createArmorPiece(config.items.chestplate, null),
            this.createArmorPiece(config.items.leggings, null),
            this.createArmorPiece(config.items.boots, null),
            config.levelThresholds.stream().mapToLong(Long::longValue).toArray(),
            mapUpgrades(typeId, config.upgrades),
            mapMaterials(config.blockedMaterials),
            mapMaterials(config.allowedMaterials),
            config.respectSpeedEnchants,
            statusTexts,
            work
        );
    }

    private ToolRequirement mapToolRequirement(AbstractMinionTypeConfig config) {
        Set<Material> allowedMaterials = mapMaterials(config.tool.allowedMaterials);
        ToolCategory category = config.tool.category == ToolCategory.NONE ? ToolCategory.ANY : config.tool.category;
        return new ToolRequirement(category, allowedMaterials, config.tool.minDurabilityToKeep, config.tool.required);
    }

    // Materials missing on this server version are skipped so cross-version lists stay usable.
    private static Set<Material> mapMaterials(List<XMaterial> materials) {
        Set<Material> mapped = EnumSet.noneOf(Material.class);
        for (XMaterial material : materials) {
            Material parsed = material.parseMaterial();
            if (parsed != null) {
                mapped.add(parsed);
            }
        }
        return mapped;
    }

    private static Map<MinionUpgradeKind, MinionUpgradeTier[]> mapUpgrades(
        String typeId,
        Map<MinionUpgradeKind, List<MinionUpgradeTierConfig>> upgrades
    ) {
        Map<MinionUpgradeKind, MinionUpgradeTier[]> mapped = new EnumMap<>(MinionUpgradeKind.class);
        for (Map.Entry<MinionUpgradeKind, List<MinionUpgradeTierConfig>> entry : upgrades.entrySet()) {
            List<MinionUpgradeTierConfig> tierConfigs = entry.getValue();
            MinionUpgradeTier[] tiers = new MinionUpgradeTier[tierConfigs.size()];
            for (int index = 0; index < tiers.length; index++) {
                MinionUpgradeTierConfig tierConfig = tierConfigs.get(index);
                Material costMaterial = tierConfig.costMaterial.parseMaterial();
                if (costMaterial == null) {
                    throw new IllegalArgumentException("Minion type " + typeId + " upgrade " + entry.getKey()
                        + " uses cost material unavailable on this server version: " + tierConfig.costMaterial);
                }
                tiers[index] = new MinionUpgradeTier(
                    tierConfig.requiredLevel, tierConfig.value, costMaterial, tierConfig.costAmount);
            }
            mapped.put(entry.getKey(), tiers);
        }
        return mapped;
    }

    private ItemStack createHeadItem(String headTexture) {
        ItemStack head = XMaterial.PLAYER_HEAD.parseItem();
        if (headTexture.isEmpty()) {
            return head;
        }

        SkullMeta meta = (SkullMeta) head.getItemMeta();
        UUID profileId = UUID.nameUUIDFromBytes(headTexture.getBytes(StandardCharsets.UTF_8));
        PlayerProfile profile = this.server.createProfile(profileId, "minion");
        profile.setProperty(new ProfileProperty("textures", headTexture));
        meta.setPlayerProfile(profile);
        head.setItemMeta(meta);
        return head;
    }

    private ItemStack createArmorPiece(MinionArmorPieceConfig piece, ItemStack headItem) {
        XMaterial material = piece.type;
        if (material == null || material == XMaterial.AIR) {
            return null;
        }
        if (material == XMaterial.PLAYER_HEAD && headItem != null) {
            return headItem;
        }

        ItemStack item = material.parseItem();
        if (item == null) {
            throw new IllegalArgumentException("Material " + material + " is unavailable on this server version");
        }

        ItemMeta meta = item.getItemMeta();
        if (meta instanceof LeatherArmorMeta leatherMeta) {
            leatherMeta.setColor(piece.color);
        }
        meta.setEnchantmentGlintOverride(piece.glow);
        item.setItemMeta(meta);
        return item;
    }
}
