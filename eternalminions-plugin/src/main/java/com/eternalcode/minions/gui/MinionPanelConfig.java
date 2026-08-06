package com.eternalcode.minions.gui;

import com.cryptomorin.xseries.XMaterial;
import com.eternalcode.minions.config.ConfigurationFile;
import com.eternalcode.minions.config.MinionPanelAction;
import com.eternalcode.minions.config.MinionPanelElementConfig;
import com.eternalcode.minions.minion.upgrade.DefaultUpgradeKinds;
import com.eternalcode.minions.minion.upgrade.UpgradeKind;
import eu.okaeri.configs.annotation.Comment;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MinionPanelConfig extends ConfigurationFile {

    @Override
    public Path resolve(Path dataDirectory) {
        return dataDirectory.resolve("panel.yml");
    }

    @Comment("Text displayed instead of a value when a minion reached its maximum level.")
    public String maximumValue = "MAX";

    @Comment("Text displayed when an upgrade has no next level.")
    public String unavailableValue = "-";

    @Comment("Title supports MiniMessage and minion placeholders.")
    public String title = "<dark_gray>Minion <green>#{MINION_ID}";

    @Comment("Each row contains exactly 9 symbols. The number of rows defines the inventory size.")
    public List<String> pattern = List.of(
        "#########",
        "####T####",
        "#SSSISSS#",
        "###SSS###",
        "#RU#C#PL#"
    );

    @Comment("Inventory slot occupied by the profession usage instructions.")
    public int usageInstructionsSlot = 4;

    @Comment("Status texts used by the {MINION_CHEST} placeholder.")
    public String chestLinkedStatus = "Linked";
    public String chestNotLinkedStatus = "Not linked";

    @Comment("Texts used by the {MINION_DIRECTION} placeholder.")
    public String directionSouth = "South";
    public String directionWest = "West";
    public String directionNorth = "North";
    public String directionEast = "East";

    @Comment("Title of the upgrades panel. Supports MiniMessage and minion placeholders.")
    public String upgradesTitle = "<dark_gray>Minion Upgrades";

    @Comment({
        "Icons of the upgrades panel, one per upgrade kind.",
        "Placeholders: {UPGRADE_TIER}, {UPGRADE_MAX_TIER}, {UPGRADE_VALUE}, {UPGRADE_NEXT_VALUE},",
        "{UPGRADE_REQUIRED_LEVEL}, {UPGRADE_COST} (MAX when fully upgraded)."
    })
    public Map<UpgradeKind, MinionPanelElementConfig> upgradeElements = defaultUpgradeElements();

    private static Map<UpgradeKind, MinionPanelElementConfig> defaultUpgradeElements() {
        Map<UpgradeKind, MinionPanelElementConfig> elements = new LinkedHashMap<>();
        elements.put(
                DefaultUpgradeKinds.SPEED, element(
            MinionPanelAction.NONE,
            XMaterial.SUGAR,
            "<yellow>Speed <white>{UPGRADE_TIER}/{UPGRADE_MAX_TIER}",
            "<gray>Work interval:",
            "<white>{UPGRADE_VALUE} ticks <dark_gray>→ <green>{UPGRADE_NEXT_VALUE} ticks",
            "",
            "<gray>Required minion level: <white>{UPGRADE_REQUIRED_LEVEL}",
            "<gray>Cost: <green>{UPGRADE_COST}",
            "<green>Click to upgrade."
        ));
        elements.put(
                DefaultUpgradeKinds.RANGE, element(
            MinionPanelAction.NONE,
            XMaterial.SPYGLASS,
            "<yellow>Range <white>{UPGRADE_TIER}/{UPGRADE_MAX_TIER}",
            "<gray>Work radius:",
            "<white>{UPGRADE_VALUE} <dark_gray>→ <green>{UPGRADE_NEXT_VALUE}",
            "",
            "<gray>Required minion level: <white>{UPGRADE_REQUIRED_LEVEL}",
            "<gray>Cost: <green>{UPGRADE_COST}",
            "<green>Click to upgrade."
        ));
        elements.put(
                DefaultUpgradeKinds.CAPACITY, element(
            MinionPanelAction.NONE,
            XMaterial.CHEST,
            "<yellow>Capacity <white>{UPGRADE_TIER}/{UPGRADE_MAX_TIER}",
            "<gray>Storage slots:",
            "<white>{UPGRADE_VALUE} <dark_gray>→ <green>{UPGRADE_NEXT_VALUE}",
            "",
            "<gray>Required minion level: <white>{UPGRADE_REQUIRED_LEVEL}",
            "<gray>Cost: <green>{UPGRADE_COST}",
            "<green>Click to upgrade."
        ));
        return elements;
    }

    @Comment("Every symbol used by pattern must have one complete element definition.")
    public Map<Character, MinionPanelElementConfig> elements = defaultElements();

    private static Map<Character, MinionPanelElementConfig> defaultElements() {
        Map<Character, MinionPanelElementConfig> elements = new LinkedHashMap<>();
        elements.put('#', element(MinionPanelAction.NONE, XMaterial.BLACK_STAINED_GLASS_PANE, " "));
        elements.put('T', element(
            MinionPanelAction.TOOL_SLOT,
            XMaterial.GRAY_DYE,
            "<red>No tool equipped",
            "<gray>Click this slot while holding a tool."
        ));
        elements.put('S', element(
            MinionPanelAction.STORAGE_SLOT,
            XMaterial.GRAY_STAINED_GLASS_PANE,
            "<dark_gray>Empty storage slot"
        ));
        elements.put('I', element(
            MinionPanelAction.MINION_INFORMATION,
            XMaterial.BOOK,
            "<green>Minion Information",
            "<gray>Profession: <white>{MINION_BEHAVIOR}",
            "<gray>Level: <white>{MINION_LEVEL}/{MINION_MAX_LEVEL}",
            "<gray>Progress to the next level:",
            "{MINION_PROGRESS_BAR} <white>{MINION_PROGRESS}<gray>/<white>{MINION_PROGRESS_REQUIRED}",
            "<gray>Storage: <white>{STORAGE_USED}/{STORAGE_CAPACITY}"
        ));
        elements.put('U', element(
            MinionPanelAction.UPGRADES,
            XMaterial.EXPERIENCE_BOTTLE,
            "<yellow>Upgrades",
            "<gray>Click to open the upgrades panel."
        ));
        elements.put('L', element(
            MinionPanelAction.LINK_CHEST,
            XMaterial.HOPPER,
            "<yellow>Linked Chest",
            "<gray>Chest: <white>{MINION_CHEST}",
            "<gray>Click to link or unlink a chest."
        ));
        elements.put('R', element(
            MinionPanelAction.ROTATE,
            XMaterial.COMPASS,
            "<yellow>Rotation",
            "<gray>Direction: <white>{MINION_DIRECTION}",
            "<gray>Click to rotate by 90°."
        ));
        elements.put('C', element(
            MinionPanelAction.COLLECT_ITEMS,
            XMaterial.CHEST,
            "<green>Collect Items",
            "<gray>Storage: <white>{STORAGE_USED}/{STORAGE_CAPACITY}"
        ));
        elements.put('P', element(
            MinionPanelAction.PICKUP_MINION,
            XMaterial.BARRIER,
            "<red>Pick Up Minion"
        ));
        return elements;
    }

    private static MinionPanelElementConfig element(
        MinionPanelAction action,
        XMaterial material,
        String displayName,
        String... lore
    ) {
        MinionPanelElementConfig element = new MinionPanelElementConfig();
        element.action = action;
        element.material = material;
        element.displayName = displayName;
        element.lore = List.of(lore);
        return element;
    }
}
