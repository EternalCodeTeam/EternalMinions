package com.eternalcode.minions.config;

import com.cryptomorin.xseries.XMaterial;
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
        "###C#P###"
    );

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
            "<gray>Poziom: <white>{MINION_LEVEL}",
            "<gray>Magazyn: <white>{STORAGE_USED}/{STORAGE_CAPACITY}"
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
