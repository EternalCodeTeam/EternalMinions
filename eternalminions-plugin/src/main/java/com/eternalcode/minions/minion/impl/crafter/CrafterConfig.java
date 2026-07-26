package com.eternalcode.minions.minion.impl.crafter;

import com.eternalcode.minions.config.AbstractMinionConfig;
import com.eternalcode.minions.minion.status.MinionStatus;
import com.eternalcode.minions.minion.tool.ToolCategory;
import eu.okaeri.configs.annotation.Include;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Color;

@Include(AbstractMinionConfig.class)
public final class CrafterConfig extends AbstractMinionConfig {

    public CrafterConfig() {
        this.displayName = "<gold>Crafter";

        this.tool.category = ToolCategory.ANY;
        this.tool.required = false;

        this.items.helmet.texture =
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzJjZGMwZmViNzAwMWUyYzEwZmQ1MDY2ZTUwMWI4N2UzZDY0NzkzMDkyYjg1YTUwYzg1NmQ5NjJmOGJlOTJjNzgifX19";

        this.npcSkin =
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHBzOi8vcy5uYW1lbWMuY29tL2kvMjEwNWZkZDUxMzNkZjI3Ni5wbmcifX19";

        this.items.setLeatherArmorColor(
                Color.fromRGB(125, 75, 30)
        );

        this.statuses = defaultStatuses();
        this.usageInstructions.lore = List.of(
            "<gray>1. Podepnij skrzynię do miniona.",
            "<gray>2. Włóż do niej składniki receptur.",
            "<gray>3. Minion automatycznie wykona dostępne receptury.",
            "<gray>4. Zostaw miejsce na gotowe przedmioty."
        );
    }

    private static Map<MinionStatus, String> defaultStatuses() {
        Map<MinionStatus, String> statuses =
                new LinkedHashMap<>();

        statuses.put(
                CrafterStatuses.CRAFTING,
                "<green>Tworzenie..."
        );

        statuses.put(
                CrafterStatuses.NO_RECIPE_SELECTED,
                "<red>Brak receptury dla wybranego przedmiotu"
        );

        statuses.put(
                CrafterStatuses.NO_CHEST,
                "<red>Brak podpiętej skrzyni"
        );

        statuses.put(
                CrafterStatuses.NO_INGREDIENTS,
                "<yellow>Brak składników"
        );

        statuses.put(
                CrafterStatuses.NO_ROOM_FOR_RESULT,
                "<red>Brak miejsca na rezultat"
        );

        return statuses;
    }
}
