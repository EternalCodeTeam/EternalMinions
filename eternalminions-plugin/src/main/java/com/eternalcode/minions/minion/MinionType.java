package com.eternalcode.minions.minion;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class MinionType {

    private final String id;
    private final String displayName;
    private final MinionBehaviorType behavior;
    private final int workIntervalTicks;
    private final int idleIntervalTicks;
    private final int storageCapacity;
    private final double npcScale;
    private final String headTexture;
    private final ItemStack headItem;
    private final ItemStack helmet;
    private final ItemStack chestplate;
    private final ItemStack leggings;
    private final ItemStack boots;
    private final long[] levelThresholds;
    private final Map<MinionUpgradeKind, MinionUpgradeTier[]> upgrades;
    private final Set<Material> blockedMaterials;
    private final Set<Material> allowedMaterials;
    private final MinionWork work;

    public MinionType(
        String id,
        String displayName,
        MinionBehaviorType behavior,
        int workIntervalTicks,
        int idleIntervalTicks,
        int storageCapacity,
        double npcScale,
        String headTexture,
        ItemStack headItem,
        ItemStack helmet,
        ItemStack chestplate,
        ItemStack leggings,
        ItemStack boots,
        long[] levelThresholds,
        Map<MinionUpgradeKind, MinionUpgradeTier[]> upgrades,
        Set<Material> blockedMaterials,
        Set<Material> allowedMaterials,
        MinionWork work
    ) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Minion type id must not be blank");
        }
        if (displayName == null || headTexture == null || headItem == null) {
            throw new IllegalArgumentException("Minion type " + id + " requires display name, texture and head item");
        }
        if (behavior == null) {
            throw new IllegalArgumentException("Minion type " + id + " requires a behavior");
        }
        if (workIntervalTicks < 1 || idleIntervalTicks < 1) {
            throw new IllegalArgumentException("Minion type " + id + " intervals must be positive");
        }
        if (storageCapacity < 1 || storageCapacity > 54) {
            throw new IllegalArgumentException("Minion type " + id + " storage capacity must be between 1 and 54");
        }
        if (npcScale < 0.05D || npcScale > 10.0D) {
            throw new IllegalArgumentException("Minion type " + id + " npc scale must be between 0.05 and 10");
        }
        if (levelThresholds == null) {
            throw new IllegalArgumentException("Minion type " + id + " requires level thresholds");
        }
        for (int index = 0; index < levelThresholds.length; index++) {
            long threshold = levelThresholds[index];
            boolean ascending = index == 0 || threshold > levelThresholds[index - 1];
            if (threshold < 1 || !ascending) {
                throw new IllegalArgumentException(
                    "Minion type " + id + " level thresholds must be positive and strictly ascending");
            }
        }
        this.id = id;
        this.displayName = displayName;
        this.behavior = behavior;
        this.workIntervalTicks = workIntervalTicks;
        this.idleIntervalTicks = idleIntervalTicks;
        this.storageCapacity = storageCapacity;
        this.npcScale = npcScale;
        this.headTexture = headTexture;
        this.headItem = headItem.clone();
        this.helmet = helmet == null ? null : helmet.clone();
        this.chestplate = chestplate == null ? null : chestplate.clone();
        this.leggings = leggings == null ? null : leggings.clone();
        this.boots = boots == null ? null : boots.clone();
        this.levelThresholds = levelThresholds.clone();
        this.upgrades = validateUpgrades(id, upgrades);
        if (blockedMaterials == null) {
            throw new IllegalArgumentException("Minion type " + id + " requires a blocked material set");
        }
        this.blockedMaterials = blockedMaterials.isEmpty()
            ? EnumSet.noneOf(Material.class)
            : EnumSet.copyOf(blockedMaterials);
        this.allowedMaterials = allowedMaterials == null || allowedMaterials.isEmpty()
            ? null
            : EnumSet.copyOf(allowedMaterials);
        if (work == null) {
            throw new IllegalArgumentException("Minion type " + id + " requires work settings");
        }
        this.work = work;
    }

    public MinionWork work() {
        return this.work;
    }

    public boolean canMine(Material material) {
        if (material == Material.BEDROCK || this.blockedMaterials.contains(material)) {
            return false;
        }
        return this.allowedMaterials == null || this.allowedMaterials.contains(material);
    }

    private static Map<MinionUpgradeKind, MinionUpgradeTier[]> validateUpgrades(
        String id,
        Map<MinionUpgradeKind, MinionUpgradeTier[]> upgrades
    ) {
        if (upgrades == null) {
            throw new IllegalArgumentException("Minion type " + id + " requires an upgrade map");
        }
        Map<MinionUpgradeKind, MinionUpgradeTier[]> validated = new EnumMap<>(MinionUpgradeKind.class);
        for (Map.Entry<MinionUpgradeKind, MinionUpgradeTier[]> entry : upgrades.entrySet()) {
            MinionUpgradeTier[] tiers = entry.getValue().clone();
            for (MinionUpgradeTier tier : tiers) {
                boolean rangeValid = entry.getKey() != MinionUpgradeKind.RANGE || tier.value() <= 3;
                boolean capacityValid = entry.getKey() != MinionUpgradeKind.CAPACITY || tier.value() <= 54;
                if (!rangeValid || !capacityValid) {
                    throw new IllegalArgumentException(
                        "Minion type " + id + " upgrade " + entry.getKey() + " has value outside safe limits");
                }
            }
            validated.put(entry.getKey(), tiers);
        }
        return validated;
    }

    public int maxLevel() {
        return this.levelThresholds.length + 1;
    }

    public int maxUpgradeTier(MinionUpgradeKind kind) {
        MinionUpgradeTier[] tiers = this.upgrades.get(kind);
        return tiers == null ? 0 : tiers.length;
    }

    public MinionUpgradeTier upgradeTier(MinionUpgradeKind kind, int tier) {
        MinionUpgradeTier[] tiers = this.upgrades.get(kind);
        if (tiers == null || tier < 1 || tier > tiers.length) {
            throw new IllegalArgumentException(
                "Upgrade " + kind + " tier " + tier + " does not exist for minion type " + this.id);
        }
        return tiers[tier - 1];
    }

    public long workIntervalTicks(MinionUpgrades minionUpgrades) {
        int tier = Math.min(minionUpgrades.tier(MinionUpgradeKind.SPEED), this.maxUpgradeTier(MinionUpgradeKind.SPEED));
        return tier == 0 ? this.workIntervalTicks : this.upgradeTier(MinionUpgradeKind.SPEED, tier).value();
    }

    public int miningRadius(MinionUpgrades minionUpgrades) {
        int tier = Math.min(minionUpgrades.tier(MinionUpgradeKind.RANGE), this.maxUpgradeTier(MinionUpgradeKind.RANGE));
        return tier == 0 ? 1 : this.upgradeTier(MinionUpgradeKind.RANGE, tier).value();
    }

    public int storageCapacity(MinionUpgrades minionUpgrades) {
        int tier = Math.min(minionUpgrades.tier(MinionUpgradeKind.CAPACITY), this.maxUpgradeTier(MinionUpgradeKind.CAPACITY));
        return tier == 0 ? this.storageCapacity : this.upgradeTier(MinionUpgradeKind.CAPACITY, tier).value();
    }

    public long progressToReach(int level) {
        if (level < 2 || level > this.maxLevel()) {
            throw new IllegalArgumentException(
                "Level " + level + " is outside range 2-" + this.maxLevel() + " of minion type " + this.id);
        }
        return this.levelThresholds[level - 2];
    }

    public String id() {
        return this.id;
    }

    public String displayName() {
        return this.displayName;
    }

    public MinionBehaviorType behavior() {
        return this.behavior;
    }

    public int workIntervalTicks() {
        return this.workIntervalTicks;
    }

    public int idleIntervalTicks() {
        return this.idleIntervalTicks;
    }

    public int storageCapacity() {
        return this.storageCapacity;
    }

    public double npcScale() {
        return this.npcScale;
    }

    public String headTexture() {
        return this.headTexture;
    }

    public ItemStack headItem() {
        return this.headItem.clone();
    }

    public ItemStack helmet() {
        return this.helmet == null ? null : this.helmet.clone();
    }

    public ItemStack chestplate() {
        return this.chestplate == null ? null : this.chestplate.clone();
    }

    public ItemStack leggings() {
        return this.leggings == null ? null : this.leggings.clone();
    }

    public ItemStack boots() {
        return this.boots == null ? null : this.boots.clone();
    }
}
