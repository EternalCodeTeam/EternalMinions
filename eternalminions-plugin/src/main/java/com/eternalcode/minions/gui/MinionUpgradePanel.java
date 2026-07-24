package com.eternalcode.minions.gui;

import com.eternalcode.minions.config.MinionPanelConfig;
import com.eternalcode.minions.config.MinionPanelElementConfig;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionType;
import com.eternalcode.minions.minion.MinionTypeService;
import com.eternalcode.minions.minion.MinionUpgradeKind;
import com.eternalcode.minions.minion.MinionUpgradeService;
import com.eternalcode.minions.minion.MinionUpgradeTier;
import com.eternalcode.minions.minion.MinionUpgrades;
import com.github.stefvanschie.inventoryframework.adventuresupport.ComponentHolder;
import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.github.stefvanschie.inventoryframework.pane.util.Slot;
import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class MinionUpgradePanel {

    private static final Map<MinionUpgradeKind, Integer> COLUMNS = Map.of(
        MinionUpgradeKind.SPEED, 2,
        MinionUpgradeKind.RANGE, 4,
        MinionUpgradeKind.CAPACITY, 6
    );

    private final Plugin plugin;
    private final MinionPanelConfig config;
    private final MiniMessage miniMessage;
    private final MinionTypeService types;
    private final MinionUpgradeService upgrades;
    private final PanelItemFactory items;

    public MinionUpgradePanel(
        Plugin plugin,
        MinionPanelConfig config,
        MiniMessage miniMessage,
        MinionTypeService types,
        MinionUpgradeService upgrades
    ) {
        this.plugin = plugin;
        this.config = config;
        this.miniMessage = miniMessage;
        this.types = types;
        this.upgrades = upgrades;
        this.items = new PanelItemFactory(miniMessage);
    }

    public void open(Player player, Minion minion) {
        MinionType type = this.types.type(minion.behaviorId()).orElse(null);
        if (type == null) {
            return;
        }

        ChestGui gui = new ChestGui(
            3,
            ComponentHolder.of(this.miniMessage.deserialize(this.config.upgradesTitle)),
            this.plugin
        );
        gui.setOnGlobalClick(event -> event.setCancelled(true));
        gui.setOnGlobalDrag(event -> event.setCancelled(true));

        StaticPane pane = new StaticPane(9, 3);
        for (Map.Entry<MinionUpgradeKind, MinionPanelElementConfig> entry : this.config.upgradeElements.entrySet()) {
            MinionUpgradeKind kind = entry.getKey();
            Integer column = COLUMNS.get(kind);
            if (column == null) {
                continue;
            }

            Map<String, String> placeholders = this.createPlaceholders(type, minion.upgrades(), kind);
            GuiItem item = new GuiItem(this.items.create(entry.getValue(), placeholders), event -> {
                this.upgrades.purchase(player, minion, kind)
                    .ifPresent(updated -> this.open(player, updated));
            }, this.plugin);
            pane.addItem(item, column, 1);
        }

        gui.addPane(Slot.fromIndex(0), pane);
        gui.show(player);
    }

    private Map<String, String> createPlaceholders(MinionType type, MinionUpgrades minionUpgrades, MinionUpgradeKind kind) {
        int tier = minionUpgrades.tier(kind);
        int maxTier = type.maxUpgradeTier(kind);
        MinionUpgradeTier nextTier = tier < maxTier ? type.upgradeTier(kind, tier + 1) : null;

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{UPGRADE_TIER}", Integer.toString(tier));
        placeholders.put("{UPGRADE_MAX_TIER}", Integer.toString(maxTier));
        placeholders.put("{UPGRADE_VALUE}", Long.toString(this.effectiveValue(type, minionUpgrades, kind)));
        placeholders.put("{UPGRADE_NEXT_VALUE}", nextTier == null ? "MAX" : Integer.toString(nextTier.value()));
        placeholders.put("{UPGRADE_REQUIRED_LEVEL}", nextTier == null ? "-" : Integer.toString(nextTier.requiredLevel()));
        placeholders.put("{UPGRADE_COST_AMOUNT}", nextTier == null ? "-" : Integer.toString(nextTier.costAmount()));
        placeholders.put("{UPGRADE_COST_MATERIAL}", nextTier == null ? "-" : nextTier.costMaterial().name());
        return placeholders;
    }

    private long effectiveValue(MinionType type, MinionUpgrades minionUpgrades, MinionUpgradeKind kind) {
        return switch (kind) {
            case SPEED -> type.workIntervalTicks(minionUpgrades);
            case RANGE -> type.miningRadius(minionUpgrades);
            case CAPACITY -> type.storageCapacity(minionUpgrades);
        };
    }
}
