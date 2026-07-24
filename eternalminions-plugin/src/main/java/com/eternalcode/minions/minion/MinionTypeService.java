package com.eternalcode.minions.minion;

import com.cryptomorin.xseries.XMaterial;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.eternalcode.minions.config.ConfigService;
import com.eternalcode.minions.config.MinionDropConfig;
import com.eternalcode.minions.config.MinionTypeConfig;
import com.eternalcode.minions.config.MinionUpgradeTierConfig;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;

public final class MinionTypeService {

    private static final String DEFAULT_TYPE_ID = "miner";

    private record TypeSeed(String id, String displayName, String headTexture, String armorColor,
                            Consumer<MinionTypeConfig> work) {
    }

    private static final List<TypeSeed> DEFAULT_TYPES = List.of(
        new TypeSeed(
            "miner",
            "<gray>Górnik",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzk2MjdiZTYyY2VkNzE0MTEzOWQzZjE1NTc5MGE1ZDQzNTZlYjdiOWVlOTVlNTA0YjMzMjI5NzRjYmM1MTVlYSJ9fX0=",
            "#C8C8C8",
            config -> config.behavior = MinionBehaviorType.MINER
        ),
        new TypeSeed(
            "lumberjack",
            "<aqua>Drwal",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmNjMGE5MTk5MGU3NmM2ZDY1ODA1MGI3YWM3ZTQ4MmJjNTgyYjI0NTg5YmI3ZjE0NmJkMWMwM2I5Yzg0Y2RkOSJ9fX0=",
            "#00C8FF",
            config -> {
                config.behavior = MinionBehaviorType.LUMBERJACK;
                config.drops = List.of(
                    new MinionDropConfig(XMaterial.OAK_LOG, 1, 2, 1.0),
                    new MinionDropConfig(XMaterial.OAK_SAPLING, 1, 1, 0.1),
                    new MinionDropConfig(XMaterial.APPLE, 1, 1, 0.03)
                );
            }
        ),
        new TypeSeed(
            "farmer",
            "<green>Rolnik",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDAxZTAzNWEzZDhkNjEyNjA3MmJjYmU1MmE5NzkxM2FjZTkzNTUyYTk5OTk1YjVkNDA3MGQ2NzgzYTMxZTkwOSJ9fX0=",
            "#00FF00",
            config -> {
                config.behavior = MinionBehaviorType.FARMER;
                config.drops = List.of(
                    new MinionDropConfig(XMaterial.WHEAT, 1, 2, 1.0),
                    new MinionDropConfig(XMaterial.WHEAT_SEEDS, 1, 2, 0.5)
                );
            }
        ),
        new TypeSeed(
            "fisherman",
            "<dark_aqua>Rybak",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWMxNWU1ZmI1NmZhMTZiMDc0N2IxYmNiMDUzMzVmNTVkMWZhMzE1NjFjMDgyYjVlMzY0M2RiNTU2NTQxMDg1MiJ9fX0=",
            "#00FFFF",
            config -> {
                config.behavior = MinionBehaviorType.FISHERMAN;
                config.requiresWater = true;
                config.drops = List.of(
                    new MinionDropConfig(XMaterial.COD, 1, 1, 0.7),
                    new MinionDropConfig(XMaterial.SALMON, 1, 1, 0.2),
                    new MinionDropConfig(XMaterial.PUFFERFISH, 1, 1, 0.05),
                    new MinionDropConfig(XMaterial.PRISMARINE_SHARD, 1, 1, 0.05)
                );
            }
        ),
        new TypeSeed(
            "collector",
            "<yellow>Zbieracz",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2UzZDM2MzVjZTQxMWFiZjFlNGYzNzNkMTYxZDA3YjhjNDdlMzU5YjZjNTZmNzRiNDEzY2I0OTRhYzc0NmUyZCJ9fX0=",
            "#FFFF00",
            config -> config.behavior = MinionBehaviorType.COLLECTOR
        ),
        new TypeSeed(
            "crafter",
            "<gold>Crafter",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmNkYzBmZWI3MDAxZTJjMTBmZDUwNjZlNTAxYjg3ZTNkNjQ3OTMwOTJiODVhNTBjODU2ZDk2MmY4YmU5MmM3OCJ9fX0=",
            "#7D4B1E",
            config -> {
                config.behavior = MinionBehaviorType.CRAFTER;
                config.drops = List.of(new MinionDropConfig(XMaterial.STICK, 1, 2, 1.0));
            }
        ),
        new TypeSeed(
            "seller",
            "<light_purple>Sprzedawca",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDI0YmE3NjBhNjFkZDI1NmM1MmIzMjUxMjlmNDYwMTZhZTg5MjIzMmEwZGVhMTcxNWY5OTdmN2M0ZDYyMmJlZiJ9fX0=",
            "#C800FF",
            config -> {
                config.behavior = MinionBehaviorType.SELLER;
                config.sellPrices = Map.of(
                    XMaterial.COBBLESTONE, 1.0,
                    XMaterial.OAK_LOG, 2.0,
                    XMaterial.WHEAT, 3.0,
                    XMaterial.COD, 4.0,
                    XMaterial.ROTTEN_FLESH, 1.5
                );
            }
        ),
        new TypeSeed(
            "killer",
            "<red>Zabójca",
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTc1MzJlOTBjNTczYTM5NGM3ODAyYWE0MTU4MzA1ODAyYjU5ZTY3ZjJhMmI3ZTNmZDAzNjNhYTZlYTQyYjg0MSJ9fX0=",
            "#FF0000",
            config -> {
                config.behavior = MinionBehaviorType.KILLER;
                config.drops = List.of(
                    new MinionDropConfig(XMaterial.ROTTEN_FLESH, 1, 2, 1.0),
                    new MinionDropConfig(XMaterial.IRON_INGOT, 1, 1, 0.02),
                    new MinionDropConfig(XMaterial.EMERALD, 1, 1, 0.01)
                );
            }
        )
    );

    private final Server server;
    private final Map<String, MinionTypeConfig> typeConfigs = new LinkedHashMap<>();
    private Map<String, MinionType> types = Map.of();

    public MinionTypeService(ConfigService configs, Server server, File directory) {
        this.server = server;
        this.loadConfigs(configs, directory);
        this.rebuild();
    }

    public Optional<MinionType> type(String typeId) {
        return Optional.ofNullable(this.types.get(typeId));
    }

    public MinionType defaultType() {
        MinionType defaultType = this.types.get(DEFAULT_TYPE_ID);
        if (defaultType != null) {
            return defaultType;
        }
        return this.types.values().iterator().next();
    }

    public Collection<String> typeIds() {
        return this.types.keySet();
    }

    public void rebuild() {
        Map<String, MinionType> rebuilt = new LinkedHashMap<>();
        for (Map.Entry<String, MinionTypeConfig> entry : this.typeConfigs.entrySet()) {
            rebuilt.put(entry.getKey(), this.map(entry.getKey(), entry.getValue()));
        }
        this.types = Map.copyOf(rebuilt);
    }

    private void loadConfigs(ConfigService configs, File directory) {
        directory.mkdirs();
        File[] files = directory.listFiles((ignored, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            for (TypeSeed seed : DEFAULT_TYPES) {
                MinionTypeConfig config = configs.create(
                    MinionTypeConfig.class,
                    new File(directory, seed.id() + ".yml"),
                    typeConfig -> {
                        typeConfig.displayName = seed.displayName();
                        typeConfig.headTexture = seed.headTexture();
                        typeConfig.armorColor = seed.armorColor();
                        seed.work().accept(typeConfig);
                    }
                );
                this.typeConfigs.put(seed.id(), config);
            }
            return;
        }

        Arrays.sort(files, Comparator.comparing(File::getName));
        for (File file : files) {
            String typeId = file.getName().substring(0, file.getName().length() - ".yml".length());
            this.typeConfigs.put(typeId, configs.create(MinionTypeConfig.class, file));
        }
    }

    private MinionType map(String typeId, MinionTypeConfig config) {
        Color armorColor = parseColor(typeId, config.armorColor);
        ItemStack headItem = this.createHeadItem(config.headTexture);
        return new MinionType(
            typeId,
            config.displayName,
            config.behavior,
            config.workIntervalTicks,
            config.idleIntervalTicks,
            config.storageCapacity,
            config.npcScale,
            config.headTexture,
            headItem,
            this.createArmorPiece(config.helmet, armorColor, headItem),
            this.createArmorPiece(config.chestplate, armorColor, null),
            this.createArmorPiece(config.leggings, armorColor, null),
            this.createArmorPiece(config.boots, armorColor, null),
            config.levelThresholds.stream().mapToLong(Long::longValue).toArray(),
            mapUpgrades(typeId, config.upgrades),
            mapMaterials(config.blockedMaterials),
            mapMaterials(config.allowedMaterials),
            mapWork(config)
        );
    }

    private static MinionWork mapWork(MinionTypeConfig config) {
        List<MinionDrop> drops = new ArrayList<>();
        for (MinionDropConfig dropConfig : config.drops) {
            Material material = dropConfig.material.parseMaterial();
            if (material != null) {
                drops.add(new MinionDrop(material, dropConfig.minAmount, dropConfig.maxAmount, dropConfig.chance));
            }
        }
        Map<Material, Double> sellPrices = new EnumMap<>(Material.class);
        for (Map.Entry<XMaterial, Double> entry : config.sellPrices.entrySet()) {
            Material material = entry.getKey().parseMaterial();
            if (material != null) {
                sellPrices.put(material, entry.getValue());
            }
        }
        return new MinionWork(drops, config.requiresWater, config.collectorRadiusBlocks, sellPrices, config.sellBatch);
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

    private ItemStack createArmorPiece(XMaterial material, Color armorColor, ItemStack headItem) {
        if (material == null || material == XMaterial.AIR) {
            return null;
        }
        if (material == XMaterial.PLAYER_HEAD && headItem != null) {
            return headItem;
        }

        ItemStack piece = material.parseItem();
        if (piece == null) {
            throw new IllegalArgumentException("Material " + material + " is unavailable on this server version");
        }

        ItemMeta meta = piece.getItemMeta();
        if (meta instanceof LeatherArmorMeta leatherMeta) {
            leatherMeta.setColor(armorColor);
            piece.setItemMeta(leatherMeta);
        }
        return piece;
    }

    private static Color parseColor(String typeId, String armorColor) {
        if (armorColor.length() != 7 || armorColor.charAt(0) != '#') {
            throw new IllegalArgumentException(
                "Minion type " + typeId + " armor-color must use #RRGGBB format, got: " + armorColor);
        }
        try {
            return Color.fromRGB(Integer.parseInt(armorColor.substring(1), 16));
        }
        catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                "Minion type " + typeId + " armor-color is not a valid hex color: " + armorColor, exception);
        }
    }
}
