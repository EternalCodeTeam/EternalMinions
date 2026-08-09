package com.eternalcode.minions.minion.behavior.impl.collector;

import com.cryptomorin.xseries.XMaterial;
import com.eternalcode.minions.config.AbstractMinionConfig;
import com.eternalcode.minions.minion.status.MinionStatus;
import com.eternalcode.minions.minion.tool.ToolCategory;
import com.eternalcode.minions.minion.upgrade.DefaultUpgradeKinds;
import com.eternalcode.minions.minion.upgrade.MinionUpgrades;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Include;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Color;

@Include(AbstractMinionConfig.class)
public final class CollectorConfig extends AbstractMinionConfig {

    @Override
    public Path resolve(Path dataDirectory) {
        return dataDirectory.resolve("minions").resolve("collector.yml");
    }

    @Comment("Maximum nearby entities inspected per cycle.")
    public int maxScannedEntitiesPerCycle = 64;

    @Comment("Maximum item stacks collected per cycle. Zero collects every inspected matching stack.")
    public int maxCollectedStacksPerCycle = 1;

    public CollectorConfig() {
        this.displayName = "<color:#FCD05C:#FFDE87:#FCD05C>ᴄᴏʟʟᴇᴄᴛᴏʀ";
        this.tool.category = ToolCategory.SHOVEL;
        this.tool.required = true;
        this.items.helmet.texture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2UzZDM2MzVjZTQxMWFiZjFlNGYzNzNkMTYxZDA3YjhjNDdlMzU5YjZjNTZmNzRiNDEzY2I0OTRhYzc0NmUyZCJ9fX0=";
        this.npcSkin = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHBzOi8vcy5uYW1lbWMuY29tL2kvODNjZGI4NmI4ZjFjYWE4MS5wbmcifX19";
        this.items.setLeatherArmorColor(Color.fromRGB(252, 208, 92));
        this.statuses = defaultStatuses();
        this.usageInstructions.lore = List.of(
            "<gray>1. Place the minion where dropped items appear.",
            "<gray>2. Insert a shovel into the tool slot.",
            "<gray>3. The minion will collect nearby dropped items.",
            "<gray>4. Link a chest or collect items from its storage."
        );
    }

    @Comment("Radius in blocks scanned for dropped items.")
    public int collectorRadiusBlocks = 4;

    @Comment("If not empty, ONLY these materials are picked up (whitelist).")
    public List<XMaterial> collectorAllowedMaterials = List.of();

    @Comment("These materials are never picked up (blacklist).")
    public List<XMaterial> collectorBlockedMaterials = List.of();

    public int radius(MinionUpgrades upgrades) {
        return this.upgradeTierValueOrHigher(upgrades, DefaultUpgradeKinds.RANGE, this.collectorRadiusBlocks);
    }

    private static Map<MinionStatus, String> defaultStatuses() {
        Map<MinionStatus, String> statuses = new LinkedHashMap<>();
        statuses.put(CollectorStatuses.COLLECTING, "<green>Collecting items...");
        statuses.put(CollectorStatuses.NO_ITEMS_ON_GROUND, "<yellow>No items nearby");
        statuses.put(CollectorStatuses.NO_SHOVEL, "<red>Shovel required");
        return statuses;
    }
}
