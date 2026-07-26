package com.eternalcode.minions.minion.impl.fisherman;

import com.eternalcode.minions.config.AbstractMinionConfig;
import com.eternalcode.minions.minion.status.MinionStatus;
import com.eternalcode.minions.minion.tool.ToolCategory;
import eu.okaeri.configs.annotation.Comment;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.Color;

public final class FishermanConfig extends AbstractMinionConfig {

    public FishermanConfig() {
        this.displayName = "<dark_aqua>Rybak";
        this.tool.category = ToolCategory.FISHING_ROD;
        this.tool.required = true;
        this.items.helmet.texture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWMxNWU1ZmI1NmZhMTZiMDc0N2IxYmNiMDUzMzVmNTVkMWZhMzE1NjFjMDgyYjVlMzY0M2RiNTU2NTQxMDg1MiJ9fX0=";
        this.npcSkin = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHBzOi8vcy5uYW1lbWMuY29tL2kvODRmMjljMTA5YjNiZjRiYS5wbmcifX19";
        this.items.chestplate.color = Color.fromRGB(0, 255, 255);
        this.items.leggings.color = Color.fromRGB(0, 255, 255);
        this.items.boots.color = Color.fromRGB(0, 255, 255);
        this.statuses = defaultStatuses();
    }

    @Comment("Minimum contiguous water blocks required nearby to start fishing.")
    public int minWaterBlocks = 4;

    @Comment("Base ticks a catch takes before Lure reduction.")
    public int baseWaitTicks = 100;

    @Comment("Ticks the wait is reduced per level of Lure on the equipped rod.")
    public int lureTicksReductionPerLevel = 20;

    private static Map<MinionStatus, String> defaultStatuses() {
        Map<MinionStatus, String> statuses = new LinkedHashMap<>();
        statuses.put(FisherStatuses.FISHING, "<green>Łowienie...");
        statuses.put(FisherStatuses.NO_WATER_NEARBY, "<red>Brak wody w pobliżu");
        statuses.put(FisherStatuses.WATER_TOO_SMALL, "<yellow>Zbiornik jest za mały");
        statuses.put(FisherStatuses.NO_ROD, "<red>Brak wędki");
        return statuses;
    }
}
