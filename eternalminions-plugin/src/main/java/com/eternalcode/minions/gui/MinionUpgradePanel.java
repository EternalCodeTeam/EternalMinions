package com.eternalcode.minions.gui;

import com.eternalcode.minions.access.MinionAccessAction;
import com.eternalcode.minions.minion.access.MinionAccessGuard;
import com.eternalcode.minions.config.MinionPanelConfig;
import com.eternalcode.minions.config.MinionPanelElementConfig;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionBehavior;
import com.eternalcode.minions.minion.MinionBehaviorRegistry;
import com.eternalcode.minions.minion.upgrade.MinionUpgradeService;
import com.eternalcode.minions.minion.upgrade.MinionUpgradeTier;
import com.eternalcode.minions.minion.upgrade.MinionUpgrades;
import com.eternalcode.minions.minion.upgrade.DefaultUpgradeKinds;
import com.eternalcode.minions.minion.upgrade.UpgradeKind;
import com.github.stefvanschie.inventoryframework.adventuresupport.ComponentHolder;
import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.github.stefvanschie.inventoryframework.pane.util.Slot;
import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class MinionUpgradePanel {

    private static final Map<UpgradeKind, Integer> COLUMNS = Map.of(
        DefaultUpgradeKinds.SPEED, 2,
        DefaultUpgradeKinds.RANGE, 4,
        DefaultUpgradeKinds.CAPACITY, 6
    );

    private final Plugin plugin;
    private final MinionPanelConfig config;
    private final MiniMessage miniMessage;
    private final MinionBehaviorRegistry behaviors;
    private final MinionUpgradeService upgrades;
    private final MinionAccessGuard access;
    private final PanelItemFactory items;

    public MinionUpgradePanel(
        Plugin plugin,
        MinionPanelConfig config,
        MiniMessage miniMessage,
        MinionBehaviorRegistry behaviors,
        MinionUpgradeService upgrades,
        MinionAccessGuard access
    ) {
        this.plugin = plugin;
        this.config = config;
        this.miniMessage = miniMessage;
        this.behaviors = behaviors;
        this.upgrades = upgrades;
        this.access = access;
        this.items = new PanelItemFactory(miniMessage);
    }

    public void open(Player player, Minion minion) {
        this.access.findAccessible(
                player,
                minion.id(),
                MinionAccessAction.OPEN_PANEL
        ).ifPresent(current -> this.openAccessible(player, current));
    }

    private void openAccessible(Player player, Minion minion) {
        MinionBehavior behavior = this.behaviors.find(minion.behaviorId()).orElse(null);
        if (behavior == null) {
            return;
        }

        ChestGui gui = new ChestGui(
            3,
            ComponentHolder.of(this.miniMessage.deserialize(this.config.upgradesTitle)
                .decoration(TextDecoration.ITALIC, false)),
            this.plugin
        );
        gui.setOnGlobalClick(event -> event.setCancelled(true));
        gui.setOnGlobalDrag(event -> event.setCancelled(true));

        StaticPane pane = new StaticPane(9, 3);
        for (Map.Entry<UpgradeKind, MinionPanelElementConfig> entry : this.config.upgradeElements.entrySet()) {
            UpgradeKind kind = entry.getKey();
            Integer column = COLUMNS.get(kind);
            if (column == null) {
                continue;
            }

            Map<String, String> placeholders = this.createPlaceholders(behavior, minion.upgrades(), kind);
            GuiItem item = new GuiItem(this.items.create(entry.getValue(), placeholders), event -> {
                this.upgrades.purchase(player, minion, kind)
                    .ifPresent(updated -> this.open(player, updated));
            }, this.plugin);
            pane.addItem(item, column, 1);
        }

        gui.addPane(Slot.fromIndex(0), pane);
        gui.show(player);
    }

    private Map<String, String> createPlaceholders(
        MinionBehavior behavior,
        MinionUpgrades minionUpgrades,
        UpgradeKind kind
    ) {
        int tier = minionUpgrades.tier(kind);
        int maxTier = behavior.config().maxUpgradeTier(kind);
        MinionUpgradeTier nextTier = tier < maxTier ? behavior.config().upgradeTier(kind, tier + 1) : null;

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{UPGRADE_TIER}", Integer.toString(tier));
        placeholders.put("{UPGRADE_MAX_TIER}", Integer.toString(maxTier));
        placeholders.put("{UPGRADE_VALUE}", Long.toString(this.effectiveValue(behavior, minionUpgrades, kind)));
        placeholders.put("{UPGRADE_NEXT_VALUE}", nextTier == null ? this.config.maximumValue : Integer.toString(nextTier.value()));
        placeholders.put("{UPGRADE_REQUIRED_LEVEL}", nextTier == null ? this.config.unavailableValue : Integer.toString(nextTier.requiredLevel()));
        placeholders.put(
            "{UPGRADE_COST}",
            nextTier == null ? this.config.unavailableValue : this.upgrades.formatCost(nextTier.costAmount())
        );
        return placeholders;
    }

    private long effectiveValue(MinionBehavior behavior, MinionUpgrades minionUpgrades, UpgradeKind kind) {
        if (kind.equals(DefaultUpgradeKinds.SPEED)) {
            return behavior.config().workInterval(minionUpgrades);
        }
        if (kind.equals(DefaultUpgradeKinds.CAPACITY)) {
            return behavior.config().storageCapacity(minionUpgrades);
        }
        int tier = minionUpgrades.tier(kind);
        return tier == 0 ? 1L : behavior.config().upgradeTier(kind, tier).value();
    }
}
