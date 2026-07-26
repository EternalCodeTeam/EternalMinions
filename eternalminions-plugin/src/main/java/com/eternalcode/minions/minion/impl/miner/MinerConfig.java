package com.eternalcode.minions.minion.impl.miner;

import com.eternalcode.minions.config.AbstractMinionConfig;
import com.eternalcode.minions.minion.status.MinionStatus;
import com.eternalcode.minions.minion.upgrade.MinionUpgrades;
import com.eternalcode.minions.config.MinionUpgradeTierConfig;
import com.eternalcode.minions.minion.upgrade.CoreUpgradeKinds;
import com.eternalcode.minions.minion.tool.ToolCategory;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import org.bukkit.Color;

public final class MinerConfig extends AbstractMinionConfig {

    public MinerConfig() {
        this.displayName = "<gray>Górnik";
        this.tool.category = ToolCategory.PICKAXE;
        this.tool.required = true;
        this.items.helmet.texture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzk2MjdiZTYyY2VkNzE0MTEzOWQzZjE1NTc5MGE1ZDQzNTZlYjdiOWVlOTVlNTA0YjMzMjI5NzRjYmM1MTVlYSJ9fX0=";
        this.npcSkin = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHBzOi8vcy5uYW1lbWMuY29tL2kvOGM3NmQ3N2Y2NTAzMDAwMy5wbmcifX19";
        this.items.chestplate.color = Color.fromRGB(200, 200, 200);
        this.items.leggings.color = Color.fromRGB(200, 200, 200);
        this.items.boots.color = Color.fromRGB(200, 200, 200);
        this.statuses = defaultStatuses();
    }

    private static Map<MinionStatus, String> defaultStatuses() {
        Map<MinionStatus, String> statuses = new LinkedHashMap<>();
        statuses.put(MinerStatuses.MINING, "<green>Kopanie...");
        statuses.put(MinerStatuses.TOOL_TOO_WEAK, "<red>Kilof jest za słaby");
        statuses.put(MinerStatuses.NO_BLOCKS_IN_RANGE, "<yellow>Brak bloków w zasięgu");
        statuses.put(MinerStatuses.NO_PICKAXE, "<red>Brak kilofa");
        return statuses;
    }

    public int radius(MinionUpgrades upgrades) {
        int purchasedTier = upgrades.tier(CoreUpgradeKinds.RANGE);
        List<MinionUpgradeTierConfig> radiusTiers = this.upgrades.get(CoreUpgradeKinds.RANGE);
        if (purchasedTier < 1 || radiusTiers == null || radiusTiers.isEmpty()) {
            return 1;
        }
        return radiusTiers.get(Math.min(purchasedTier, radiusTiers.size()) - 1).value;
    }
}
