package com.eternalcode.minions.config;

import com.cryptomorin.xseries.XMaterial;
import com.eternalcode.minions.minion.upgrade.CoreUpgradeKinds;
import com.eternalcode.minions.minion.upgrade.UpgradeKind;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MinionPanelConfig extends OkaeriConfig {

    @Comment("Title supports MiniMessage and minion placeholders.")
    public String title = "<dark_gray>Minion <green>#{MINION_ID}";

    @Comment("Each row contains exactly 9 symbols. The number of rows defines the inventory size.")
    public List<String> pattern = List.of(
        "#########",
        "####T####",
        "#SSSISSS#",
        "###SSS###",
        "#RUACPLM#"
    );

    @Comment("Inventory slot occupied by the profession usage instructions.")
    public int usageInstructionsSlot = 4;

    @Comment("Status texts used by the {MINION_STATUS} placeholder.")
    public String statusWorking = "Pracuje";
    public String statusPaused = "Pauza";

    @Comment("Status texts used by the {MINION_CHEST} placeholder.")
    public String chestLinkedStatus = "Połączona";
    public String chestNotLinkedStatus = "Brak";

    @Comment("Texts used by the {MINION_MODE} placeholder.")
    public String modeSquare = "Kwadrat";
    public String modeLinear = "Linia";

    @Comment("Texts used by the {MINION_DIRECTION} placeholder.")
    public String directionSouth = "Południe";
    public String directionWest = "Zachód";
    public String directionNorth = "Północ";
    public String directionEast = "Wschód";

    @Comment("Title of the upgrades panel. Supports MiniMessage and minion placeholders.")
    public String upgradesTitle = "<dark_gray>Ulepszenia miniona";

    @Comment({
        "Icons of the upgrades panel, one per upgrade kind.",
        "Placeholders: {UPGRADE_TIER}, {UPGRADE_MAX_TIER}, {UPGRADE_VALUE}, {UPGRADE_NEXT_VALUE},",
        "{UPGRADE_REQUIRED_LEVEL}, {UPGRADE_COST_AMOUNT}, {UPGRADE_COST_MATERIAL} (MAX gdy wykupione)."
    })
    public Map<UpgradeKind, MinionPanelElementConfig> upgradeElements = defaultUpgradeElements();

    private static Map<UpgradeKind, MinionPanelElementConfig> defaultUpgradeElements() {
        Map<UpgradeKind, MinionPanelElementConfig> elements = new LinkedHashMap<>();
        elements.put(CoreUpgradeKinds.SPEED, element(
            MinionPanelAction.NONE,
            XMaterial.SUGAR,
            "<yellow>Szybkość <white>{UPGRADE_TIER}/{UPGRADE_MAX_TIER}",
            "<gray>Cykl pracy: <white>{UPGRADE_VALUE} ticków",
            "<gray>Następny poziom: <white>{UPGRADE_NEXT_VALUE}",
            "<gray>Wymagany poziom miniona: <white>{UPGRADE_REQUIRED_LEVEL}",
            "<gray>Koszt: <white>{UPGRADE_COST_AMOUNT}x {UPGRADE_COST_MATERIAL}",
            "<green>Kliknij, aby ulepszyć."
        ));
        elements.put(CoreUpgradeKinds.RANGE, element(
            MinionPanelAction.NONE,
            XMaterial.SPYGLASS,
            "<yellow>Zasięg <white>{UPGRADE_TIER}/{UPGRADE_MAX_TIER}",
            "<gray>Promień kopania: <white>{UPGRADE_VALUE}",
            "<gray>Następny poziom: <white>{UPGRADE_NEXT_VALUE}",
            "<gray>Wymagany poziom miniona: <white>{UPGRADE_REQUIRED_LEVEL}",
            "<gray>Koszt: <white>{UPGRADE_COST_AMOUNT}x {UPGRADE_COST_MATERIAL}",
            "<green>Kliknij, aby ulepszyć."
        ));
        elements.put(CoreUpgradeKinds.CAPACITY, element(
            MinionPanelAction.NONE,
            XMaterial.CHEST,
            "<yellow>Pojemność <white>{UPGRADE_TIER}/{UPGRADE_MAX_TIER}",
            "<gray>Sloty magazynu: <white>{UPGRADE_VALUE}",
            "<gray>Następny poziom: <white>{UPGRADE_NEXT_VALUE}",
            "<gray>Wymagany poziom miniona: <white>{UPGRADE_REQUIRED_LEVEL}",
            "<gray>Koszt: <white>{UPGRADE_COST_AMOUNT}x {UPGRADE_COST_MATERIAL}",
            "<green>Kliknij, aby ulepszyć."
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
            "<red>Brak narzędzia",
            "<gray>Kliknij narzędziem na ten slot."
        ));
        elements.put('S', element(
            MinionPanelAction.STORAGE_SLOT,
            XMaterial.GRAY_STAINED_GLASS_PANE,
            "<dark_gray>Pusty slot magazynu"
        ));
        elements.put('I', element(
            MinionPanelAction.MINION_INFORMATION,
            XMaterial.BOOK,
            "<green>Informacje o minionie",
            "<gray>Typ pracy: <white>{MINION_BEHAVIOR}",
            "<gray>Poziom: <white>{MINION_LEVEL}/{MINION_MAX_LEVEL}",
            "<gray>Postęp: <white>{MINION_PROGRESS}/{MINION_PROGRESS_REQUIRED}",
            "<gray>Magazyn: <white>{STORAGE_USED}/{STORAGE_CAPACITY}",
            "<gray>Status: <white>{MINION_STATUS}"
        ));
        elements.put('A', element(
            MinionPanelAction.TOGGLE_ACTIVE,
            XMaterial.LEVER,
            "<yellow>Start / pauza",
            "<gray>Status: <white>{MINION_STATUS}",
            "<gray>Kliknij, aby przełączyć."
        ));
        elements.put('U', element(
            MinionPanelAction.UPGRADES,
            XMaterial.EXPERIENCE_BOTTLE,
            "<yellow>Ulepszenia",
            "<gray>Kliknij, aby otworzyć panel ulepszeń."
        ));
        elements.put('L', element(
            MinionPanelAction.LINK_CHEST,
            XMaterial.HOPPER,
            "<yellow>Link do skrzyni",
            "<gray>Skrzynia: <white>{MINION_CHEST}",
            "<gray>Kliknij, aby połączyć lub rozłączyć."
        ));
        elements.put('R', element(
            MinionPanelAction.ROTATE,
            XMaterial.COMPASS,
            "<yellow>Rotacja",
            "<gray>Kierunek: <white>{MINION_DIRECTION}",
            "<gray>Kliknij, aby obrócić o 90°."
        ));
        elements.put('M', element(
            MinionPanelAction.TOGGLE_MODE,
            XMaterial.IRON_PICKAXE,
            "<yellow>Tryb pracy",
            "<gray>Tryb: <white>{MINION_MODE}",
            "<gray>Kwadrat: kopie wokół siebie.",
            "<gray>Linia: kopie w kierunku patrzenia.",
            "<gray>Kliknij, aby przełączyć."
        ));
        elements.put('C', element(
            MinionPanelAction.COLLECT_ITEMS,
            XMaterial.CHEST,
            "<green>Odbierz przedmioty",
            "<gray>Zapełnienie: <white>{STORAGE_USED}/{STORAGE_CAPACITY}"
        ));
        elements.put('P', element(
            MinionPanelAction.PICKUP_MINION,
            XMaterial.BARRIER,
            "<red>Podnieś miniona"
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
