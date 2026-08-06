package com.eternalcode.minions.minion.impl.miner;

import com.eternalcode.minions.config.AbstractMinionConfig;
import com.eternalcode.minions.minion.status.MinionStatus;
import com.eternalcode.minions.minion.upgrade.MinionUpgrades;
import com.eternalcode.minions.minion.upgrade.DefaultUpgradeKinds;
import com.eternalcode.minions.minion.tool.ToolCategory;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Include;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import org.bukkit.Color;

@Include(AbstractMinionConfig.class)
public final class MinerConfig extends AbstractMinionConfig {

    @Override
    public Path resolve(Path dataDirectory) {
        return dataDirectory.resolve("minions").resolve("miner.yml");
    }

    @Comment("SQUARE mines the area below the minion. LINE mines in its facing direction.")
    public WorkMode workMode = WorkMode.LINE;

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
            "<gray>1. Insert a pickaxe into the tool slot.",
            "<gray>2. Set the minion's direction when using LINE mode.",
            "<gray>3. The minion mines blocks one level below itself.",
            "<gray>4. Collect blocks from its storage or linked chest."
        );
    }

    private static Map<MinionStatus, String> defaultStatuses() {
        Map<MinionStatus, String> statuses = new LinkedHashMap<>();
        statuses.put(MinerStatuses.MINING, "<green>Mining...");
        statuses.put(MinerStatuses.TOOL_TOO_WEAK, "<red>Pickaxe tier too low");
        statuses.put(MinerStatuses.NO_BLOCKS_IN_RANGE, "<yellow>No blocks in range");
        statuses.put(MinerStatuses.NO_PICKAXE, "<red>Pickaxe required");
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
