package com.eternalcode.minions.minion.impl.miner;

import com.eternalcode.minions.config.AbstractMinionConfig;
import com.eternalcode.minions.minion.status.MinionStatus;
import com.eternalcode.minions.minion.upgrade.MinionUpgrades;
import com.eternalcode.minions.minion.upgrade.DefaultUpgradeKinds;
import com.eternalcode.minions.minion.tool.ToolCategory;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Include;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import org.bukkit.Color;

@Include(AbstractMinionConfig.class)
public final class MinerConfig extends AbstractMinionConfig {

    @Comment("SQUARE mines the area below the minion. LINE mines in its facing direction.")
    public WorkMode workMode = WorkMode.SQUARE;

    @Comment("Maximum blocks mined per cycle. Zero mines every matching block found in the work area.")
    public int maxBlocksPerCycle = 1;

    public MinerConfig() {
        this.displayName = "<color:#B7B7B7:#D9D9D9:#B7B7B7>ᴍɪɴᴇʀ";
        this.tool.category = ToolCategory.PICKAXE;
        this.tool.required = true;
        this.items.helmet.texture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzk2MjdiZTYyY2VkNzE0MTEzOWQzZjE1NTc5MGE1ZDQzNTZlYjdiOWVlOTVlNTA0YjMzMjI5NzRjYmM1MTVlYSJ9fX0=";
        this.npcSkin = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHBzOi8vcy5uYW1lbWMuY29tL2kvOGM3NmQ3N2Y2NTAzMDAwMy5wbmcifX19";
        this.items.setLeatherArmorColor(Color.fromRGB(183, 183, 183));
        this.statuses = defaultStatuses();
        this.usageInstructions.lore = List.of(
            "<gray>1. Włóż kilof do slotu narzędzia.",
            "<gray>2. Ustaw kierunek miniona dla trybu LINE.",
            "<gray>3. Minion kopie bloki poziom niżej.",
            "<gray>4. Odbieraj bloki z magazynu lub podpiętej skrzyni."
        );
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
        return this.upgradeTierValue(upgrades, DefaultUpgradeKinds.RANGE, 1);
    }

    public enum WorkMode {

        SQUARE,
        LINE
    }
}
