package com.eternalcode.minions.minion.behavior.impl.crafter;

import com.eternalcode.minions.config.AbstractMinionConfig;
import com.eternalcode.minions.minion.status.MinionStatus;
import com.eternalcode.minions.minion.tool.ToolCategory;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Include;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Color;

@Include(AbstractMinionConfig.class)
public final class CrafterConfig extends AbstractMinionConfig {

    @Override
    public Path resolve(Path dataDirectory) {
        return dataDirectory.resolve("minions").resolve("crafter.yml");
    }

    @Comment("Maximum recipes crafted per cycle. Zero crafts until blocked by ingredients, space or safety cap.")
    public int maxCraftsPerCycle = 1;

    @Comment("Hard maximum crafts in one cycle when maxCraftsPerCycle is zero.")
    public int maximumCraftsSafetyCap = 64;

    public CrafterConfig() {
        this.displayName = "<color:#FFB900:#FFD158:#FFB900>ᴄʀᴀꜰᴛᴇʀ";

        this.tool.category = ToolCategory.ANY;
        this.tool.required = false;

        this.items.helmet.texture =
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmNkYzBmZWI3MDAxZTJjMTBmZDUwNjZlNTAxYjg3ZTNkNjQ3OTMwOTJiODVhNTBjODU2ZDk2MmY4YmU5MmM3OCJ9fX0=";

        this.npcSkin =
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHBzOi8vcy5uYW1lbWMuY29tL2kvMjEwNWZkZDUxMzNkZjI3Ni5wbmcifX19";

        this.items.setLeatherArmorColor(
                Color.fromRGB(255, 185, 0)
        );

        this.statuses = defaultStatuses();
        this.usageInstructions.lore = List.of(
            "<gray>1. Link a chest to the minion.",
            "<gray>2. Place crafting ingredients inside the chest.",
            "<gray>3. The minion will craft available recipes automatically.",
            "<gray>4. Leave enough room for the crafted items."
        );
    }

    private static Map<MinionStatus, String> defaultStatuses() {
        Map<MinionStatus, String> statuses =
                new LinkedHashMap<>();

        statuses.put(
                CrafterStatuses.CRAFTING,
                "<green>Crafting items..."
        );

        statuses.put(
                CrafterStatuses.NO_RECIPE_SELECTED,
                "<red>No recipe selected"
        );

        statuses.put(
                CrafterStatuses.NO_CHEST,
                "<red>Linked chest required"
        );

        statuses.put(
                CrafterStatuses.NO_INGREDIENTS,
                "<yellow>Missing ingredients"
        );

        return statuses;
    }
}
