package com.eternalcode.minions.minion.impl.killer;

import com.eternalcode.minions.config.AbstractMinionConfig;
import com.eternalcode.minions.minion.status.MinionStatus;
import com.eternalcode.minions.minion.tool.ToolCategory;
import eu.okaeri.configs.annotation.Comment;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Color;
import org.bukkit.entity.EntityType;

public final class KillerConfig extends AbstractMinionConfig {

    public KillerConfig() {
        this.displayName = "<red>Zabójca";
        this.tool.category = ToolCategory.WEAPON;
        this.tool.required = true;
        this.items.helmet.texture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTc1MzJlOTBjNTczYTM5NGM3ODAyYWE0MTU4MzA1ODAyYjU5ZTY3ZjJhMmI3ZTNmZDAzNjNhYTZlYTQyYjg0MSJ9fX0=";
        this.npcSkin = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHBzOi8vcy5uYW1lbWMuY29tL2kvOWMxOTM4MDVmMWY4OTQ2OC5wbmcifX19";
        this.items.chestplate.color = Color.fromRGB(255, 0, 0);
        this.items.leggings.color = Color.fromRGB(255, 0, 0);
        this.items.boots.color = Color.fromRGB(255, 0, 0);
        this.statuses = defaultStatuses();
    }

    @Comment("Mob types this minion is allowed to attack.")
    public List<EntityType> allowedMobs = List.of(EntityType.ZOMBIE, EntityType.SKELETON);

    @Comment("Radius in blocks scanned for a target.")
    public int attackRangeBlocks = 4;

    @Comment("Ticks between attacks.")
    public int attackCooldownTicks = 20;

    @Comment("Base weapon damage before Sharpness/Smite/Bane of Arthropods bonuses.")
    public double baseDamage = 1.0;

    private static Map<MinionStatus, String> defaultStatuses() {
        Map<MinionStatus, String> statuses = new LinkedHashMap<>();
        statuses.put(KillerStatuses.ATTACKING, "<green>Atakowanie...");
        statuses.put(KillerStatuses.NO_ENEMIES, "<yellow>Brak przeciwników");
        statuses.put(KillerStatuses.NO_WEAPON, "<red>Brak broni");
        return statuses;
    }
}
