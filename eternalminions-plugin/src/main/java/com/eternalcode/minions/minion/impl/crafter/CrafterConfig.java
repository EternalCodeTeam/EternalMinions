package com.eternalcode.minions.minion.impl.crafter;

import com.eternalcode.minions.config.AbstractMinionConfig;
import com.eternalcode.minions.config.MinionRecipeConfig;
import com.eternalcode.minions.minion.status.MinionStatus;
import com.eternalcode.minions.minion.tool.ToolCategory;
import eu.okaeri.configs.annotation.Comment;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Color;

public final class CrafterConfig extends AbstractMinionConfig {

    public CrafterConfig() {
        this.displayName = "<gold>Crafter";
        this.tool.category = ToolCategory.ANY;
        this.tool.required = false;
        this.items.helmet.texture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmNkYzBmZWI3MDAxZTJjMTBmZDUwNjZlNTAxYjg3ZTNkNjQ3OTMwOTJiODVhNTBjODU2ZDk2MmY4YmU5MmM3OCJ9fX0=";
        this.npcSkin = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHBzOi8vcy5uYW1lbWMuY29tL2kvMjEwNWZkZDUxMzNkZjI3Ni5wbmcifX19";
        this.items.chestplate.color = Color.fromRGB(125, 75, 30);
        this.items.leggings.color = Color.fromRGB(125, 75, 30);
        this.items.boots.color = Color.fromRGB(125, 75, 30);
        this.statuses = defaultStatuses();
    }

    @Comment("Recipes the owner can pick between.")
    public List<MinionRecipeConfig> recipes = List.of(new MinionRecipeConfig());

    private static Map<MinionStatus, String> defaultStatuses() {
        Map<MinionStatus, String> statuses = new LinkedHashMap<>();
        statuses.put(CrafterStatuses.CRAFTING, "<green>Tworzenie...");
        statuses.put(CrafterStatuses.NO_RECIPE_SELECTED, "<red>Brak receptury");
        statuses.put(CrafterStatuses.NO_CHEST, "<red>Brak podpiętej skrzyni");
        statuses.put(CrafterStatuses.NO_INGREDIENTS, "<yellow>Brak składników");
        statuses.put(CrafterStatuses.NO_ROOM_FOR_RESULT, "<red>Brak miejsca na rezultat");
        return statuses;
    }
}
