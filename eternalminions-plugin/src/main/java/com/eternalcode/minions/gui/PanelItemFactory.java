package com.eternalcode.minions.gui;

import com.eternalcode.minions.config.MinionPanelElementConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

final class PanelItemFactory {

    private final MiniMessage miniMessage;

    PanelItemFactory(MiniMessage miniMessage) {
        this.miniMessage = miniMessage;
    }

    ItemStack create(MinionPanelElementConfig config, Map<String, String> placeholders) {
        ItemStack item = config.material.parseItem();
        if (item == null) {
            throw new IllegalStateException("XMaterial " + config.material + " is unavailable on this server version");
        }

        item.setAmount(config.amount);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(this.render(config.displayName, placeholders));
        meta.lore(this.createLore(config.lore, placeholders));
        meta.setEnchantmentGlintOverride(config.glowing);
        meta.setHideTooltip(config.hideTooltip);
        if (config.customModelData > 0) {
            var customModelData = meta.getCustomModelDataComponent();
            customModelData.setFloats(List.of((float) config.customModelData));
            meta.setCustomModelDataComponent(customModelData);
        }
        item.setItemMeta(meta);
        return item;
    }

    private List<Component> createLore(List<String> lines, Map<String, String> placeholders) {
        if (lines.isEmpty()) {
            return null;
        }

        List<Component> lore = new ArrayList<>(lines.size());
        for (String line : lines) {
            lore.add(this.render(line, placeholders));
        }
        return lore;
    }

    private String format(String input, Map<String, String> placeholders) {
        String formatted = input;
        for (Map.Entry<String, String> placeholder : placeholders.entrySet()) {
            formatted = formatted.replace(placeholder.getKey(), placeholder.getValue());
        }
        return formatted;
    }

    private Component render(String input, Map<String, String> placeholders) {
        return this.miniMessage.deserialize(this.format(input, placeholders))
            .decoration(TextDecoration.ITALIC, false);
    }
}
