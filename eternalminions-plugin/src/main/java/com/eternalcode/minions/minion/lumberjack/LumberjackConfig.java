package com.eternalcode.minions.minion.lumberjack;

import com.cryptomorin.xseries.XMaterial;
import com.eternalcode.minions.config.AbstractMinionTypeConfig;
import com.eternalcode.minions.minion.status.MinionStatus;
import com.eternalcode.minions.minion.tool.ToolCategory;
import eu.okaeri.configs.annotation.Comment;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.Color;

public final class LumberjackConfig extends AbstractMinionTypeConfig {

    public LumberjackConfig() {
        this.displayName = "<aqua>Drwal";
        this.tool.category = ToolCategory.AXE;
        this.tool.required = true;
        this.items.helmet.texture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmNjMGE5MTk5MGU3NmM2ZDY1ODA1MGI3YWM3ZTQ4MmJjNTgyYjI0NTg5YmI3ZjE0NmJkMWMwM2I5Yzg0Y2RkOSJ9fX0=";
        this.npcSkin = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHBzOi8vcy5uYW1lbWMuY29tL2kvODkzNjFiMTYyMzIzYzYyYS5wbmcifX19";
        this.items.chestplate.color = Color.fromRGB(0, 200, 255);
        this.items.leggings.color = Color.fromRGB(0, 200, 255);
        this.items.boots.color = Color.fromRGB(0, 200, 255);
    }

    @Comment("Log material recognized as this type's tree.")
    public XMaterial logMaterial = XMaterial.OAK_LOG;

    @Comment("Sapling material planted at each station and replanted after felling.")
    public XMaterial saplingMaterial = XMaterial.OAK_SAPLING;

    @Comment("Maximum connected log blocks felled in one bounded scan.")
    public int maxLogsPerTree = 128;

    @Comment("Status text shown in the hologram, keyed by status name. Supports MiniMessage.")
    public Map<MinionStatus, String> statuses = defaultStatuses();

    private static Map<MinionStatus, String> defaultStatuses() {
        Map<MinionStatus, String> statuses = new LinkedHashMap<>();
        statuses.put(LumberjackStatuses.CUTTING, "<green>Ścinanie...");
        statuses.put(LumberjackStatuses.WAITING_FOR_TREE, "<yellow>Czekam na drzewo");
        statuses.put(LumberjackStatuses.NO_SAPLING, "<red>Brak sadzonki");
        statuses.put(LumberjackStatuses.INVALID_STATION, "<red>Nieprawidłowe stanowisko");
        statuses.put(LumberjackStatuses.NO_AXE, "<red>Brak siekiery");
        return statuses;
    }
}
