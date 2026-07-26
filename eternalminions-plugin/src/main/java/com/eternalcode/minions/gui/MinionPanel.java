package com.eternalcode.minions.gui;

import com.eternalcode.minions.access.MinionAccessAction;
import com.eternalcode.minions.access.MinionAccessGuard;
import com.eternalcode.minions.config.MessagesConfig;
import com.eternalcode.minions.config.MinionPanelAction;
import com.eternalcode.minions.config.MinionPanelConfig;
import com.eternalcode.minions.config.MinionPanelElementConfig;
import com.eternalcode.minions.config.MinionPanelLayout;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionBehavior;
import com.eternalcode.minions.minion.MinionBehaviorRegistry;
import com.eternalcode.minions.minion.MinionEquipment;
import com.eternalcode.minions.minion.MinionLifecycleService;
import com.eternalcode.minions.minion.storage.MinionStorage;
import com.eternalcode.minions.minion.storage.MinionItemTransferService;
import com.eternalcode.minions.minion.MiningMode;
import com.eternalcode.minions.notice.NoticeService;
import com.eternalcode.multification.notice.Notice;
import com.github.stefvanschie.inventoryframework.adventuresupport.ComponentHolder;
import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.github.stefvanschie.inventoryframework.pane.util.Slot;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public final class MinionPanel {

    private final Plugin plugin;
    private final MinionPanelConfig config;
    private final MessagesConfig messages;
    private final NoticeService notices;
    private final MiniMessage miniMessage;
    private final MinionBehaviorRegistry behaviors;
    private final PanelItemFactory items;
    private final MinionLifecycleService lifecycle;
    private final MinionAccessGuard access;
    private final MinionItemTransferService transfers;
    private final BiConsumer<Player, Minion> pickup;
    private final BiConsumer<Player, Minion> openUpgrades;
    private final BiConsumer<Player, Minion> linkChest;

    public MinionPanel(
        Plugin plugin,
        MinionPanelConfig config,
        MessagesConfig messages,
        NoticeService notices,
        MiniMessage miniMessage,
        MinionBehaviorRegistry behaviors,
        MinionLifecycleService lifecycle,
        MinionAccessGuard access,
        MinionItemTransferService transfers,
        BiConsumer<Player, Minion> pickup,
        BiConsumer<Player, Minion> openUpgrades,
        BiConsumer<Player, Minion> linkChest
    ) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.notices = notices;
        this.miniMessage = miniMessage;
        this.behaviors = behaviors;
        this.items = new PanelItemFactory(miniMessage);
        this.lifecycle = lifecycle;
        this.access = access;
        this.transfers = transfers;
        this.pickup = pickup;
        this.openUpgrades = openUpgrades;
        this.linkChest = linkChest;
    }

    public void open(Player player, Minion minion) {
        this.access.findAccessible(
                player,
                minion.id(),
                MinionAccessAction.OPEN_PANEL
        ).ifPresent(current -> this.openAccessible(player, current));
    }

    private void openAccessible(Player player, Minion minion) {
        MinionPanelLayout layout = MinionPanelLayout.from(this.config);
        Map<String, String> placeholders = this.createPlaceholders(minion);
        ChestGui gui = new ChestGui(
            layout.rows(),
            ComponentHolder.of(this.miniMessage.deserialize(this.format(this.config.title, placeholders))),
            this.plugin
        );
        gui.setOnGlobalClick(event -> {
            boolean playerInventory = event.getClickedInventory() == event.getView().getBottomInventory();
            boolean cursorClick = event.getClick() == ClickType.LEFT || event.getClick() == ClickType.RIGHT;
            event.setCancelled(!playerInventory || !cursorClick);
        });
        gui.setOnGlobalDrag(event -> event.setCancelled(true));

        StaticPane pane = new StaticPane(9, layout.rows());
        int storageIndex = 0;
        for (int row = 0; row < layout.rows(); row++) {
            String patternRow = this.config.pattern.get(row);
            for (int column = 0; column < 9; column++) {
                MinionPanelElementConfig element = this.config.elements.get(patternRow.charAt(column));
                storageIndex = this.addElement(pane, column, row, player, minion, element, placeholders, storageIndex);
            }
        }
        this.addUsageInstructions(pane, layout, minion, placeholders);

        gui.addPane(Slot.fromIndex(0), pane);
        gui.show(player);
    }

    private void addUsageInstructions(
        StaticPane pane,
        MinionPanelLayout layout,
        Minion minion,
        Map<String, String> placeholders
    ) {
        if (this.config.usageInstructionsSlot < 0 || this.config.usageInstructionsSlot >= layout.rows() * 9) {
            throw new IllegalArgumentException("Usage instructions slot is outside the minion panel");
        }

        MinionBehavior behavior = this.behaviors.find(minion.behaviorId()).orElse(null);
        if (behavior == null) {
            return;
        }

        int column = this.config.usageInstructionsSlot % 9;
        int row = this.config.usageInstructionsSlot / 9;
        ItemStack icon = this.items.create(behavior.config().usageInstructions, placeholders);
        pane.addItem(new GuiItem(icon, this.plugin), column, row);
    }

    private int addElement(
        StaticPane pane,
        int column,
        int row,
        Player player,
        Minion minion,
        MinionPanelElementConfig element,
        Map<String, String> placeholders,
        int storageIndex
    ) {
        if (element.action == MinionPanelAction.STORAGE_SLOT) {
            ItemStack stored = storageIndex < minion.storage().capacity() ? minion.storage().item(storageIndex) : null;
            ItemStack icon = stored == null ? this.items.create(element, placeholders) : stored;
            pane.addItem(new GuiItem(icon, this.plugin), column, row);
            return storageIndex + 1;
        }

        GuiItem item = switch (element.action) {
            case TOOL_SLOT -> this.createToolElement(player, minion, element, placeholders);
            case COLLECT_ITEMS -> this.createAccessibleElement(
                    player, minion, MinionAccessAction.MANAGE, element, placeholders,
                    current -> this.collect(player, current)
            );
            case PICKUP_MINION -> this.createAccessibleElement(
                    player, minion, MinionAccessAction.PICK_UP, element, placeholders,
                    current -> this.pickup.accept(player, current)
            );
            case TOGGLE_ACTIVE -> this.createAccessibleElement(
                    player, minion, MinionAccessAction.MANAGE, element, placeholders,
                    current -> this.toggleActive(player, current)
            );
            case UPGRADES -> this.createAccessibleElement(
                    player, minion, MinionAccessAction.OPEN_PANEL, element, placeholders,
                    current -> this.openUpgrades.accept(player, current)
            );
            case LINK_CHEST -> this.createAccessibleElement(
                    player, minion, MinionAccessAction.MANAGE, element, placeholders,
                    current -> this.linkChest.accept(player, current)
            );
            case ROTATE -> this.createAccessibleElement(
                    player, minion, MinionAccessAction.MANAGE, element, placeholders,
                    current -> this.rotate(player, current)
            );
            case TOGGLE_MODE -> this.createAccessibleElement(
                    player, minion, MinionAccessAction.MANAGE, element, placeholders,
                    current -> this.toggleMode(player, current)
            );
            case NONE, MINION_INFORMATION -> this.createConfiguredElement(element, placeholders, null);
            case STORAGE_SLOT -> throw new IllegalStateException("Storage action was not handled");
        };
        pane.addItem(item, column, row);
        return storageIndex;
    }

    private GuiItem createToolElement(
        Player player,
        Minion minion,
        MinionPanelElementConfig element,
        Map<String, String> placeholders
    ) {
        ItemStack tool = minion.equipment().tool();
        ItemStack icon = tool == null ? this.items.create(element, placeholders) : tool;
        return new GuiItem(icon, event -> this.withAccess(
                player,
                minion,
                MinionAccessAction.MANAGE,
                current -> this.updateTool(player, current, event.getCursor())
        ), this.plugin);
    }

    private void updateTool(Player player, Minion minion, ItemStack cursor) {
        ItemStack currentTool = minion.equipment().tool();
        Minion updated = minion.withEquipment(
                new MinionEquipment(cursor.getType().isAir() ? null : cursor)
        );
        this.lifecycle.updateEquipment(updated);
        player.setItemOnCursor(currentTool);
        this.send(player, this.messages.minionToolUpdated);
        this.refresh(player, updated);
    }

    private GuiItem createConfiguredElement(
        MinionPanelElementConfig element,
        Map<String, String> placeholders,
        Consumer<InventoryClickEvent> click
    ) {
        ItemStack item = this.items.create(element, placeholders);
        return click == null ? new GuiItem(item, this.plugin) : new GuiItem(item, click, this.plugin);
    }

    private GuiItem createAccessibleElement(
            Player player,
            Minion minion,
            MinionAccessAction action,
            MinionPanelElementConfig element,
            Map<String, String> placeholders,
            Consumer<Minion> click
    ) {
        return this.createConfiguredElement(
                element,
                placeholders,
                event -> this.withAccess(player, minion, action, click)
        );
    }

    private void toggleActive(Player player, Minion minion) {
        Minion updated = minion.withActive(!minion.active());
        this.lifecycle.updateState(updated);
        this.send(player, updated.active() ? this.messages.minionResumed : this.messages.minionPaused);
        this.refresh(player, updated);
    }

    private void rotate(Player player, Minion minion) {
        Minion updated = minion.withSettings(minion.settings().withDirection(minion.settings().direction().rotated()));
        this.lifecycle.updateSettings(updated);
        this.send(player, this.messages.minionRotated);
        this.refresh(player, updated);
    }

    private void toggleMode(Player player, Minion minion) {
        MiningMode mode = minion.settings().miningMode() == MiningMode.SQUARE ? MiningMode.LINEAR : MiningMode.SQUARE;
        Minion updated = minion.withSettings(minion.settings().withMiningMode(mode));
        this.lifecycle.updateSettings(updated);
        this.send(player, this.messages.minionModeChanged);
        this.refresh(player, updated);
    }

    private void collect(Player player, Minion minion) {
        MinionStorage storage = minion.storage();
        for (int slot = 0; slot < storage.capacity(); slot++) {
            ItemStack item = storage.item(slot);
            if (item == null) {
                continue;
            }
            ItemStack remaining = this.transfers.addToInventory(player.getInventory(), item);
            storage = storage.withItem(slot, remaining);
        }
        Minion updated = minion.withStorage(storage);
        this.lifecycle.updateStorage(updated);
        this.send(player, this.messages.minionStorageCollected);
        this.refresh(player, updated);
    }

    private Map<String, String> createPlaceholders(Minion minion) {
        MinionBehavior behavior = this.behaviors.find(minion.behaviorId()).orElse(null);
        int level = minion.progress().level();

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{MINION_ID}", Long.toString(minion.id().value()));
        placeholders.put(
            "{MINION_BEHAVIOR}",
            behavior == null ? minion.behaviorId() : behavior.config().displayName
        );
        placeholders.put("{MINION_LEVEL}", Integer.toString(level));
        placeholders.put(
            "{MINION_MAX_LEVEL}",
            behavior == null ? "?" : Integer.toString(behavior.config().maxLevel())
        );
        placeholders.put("{MINION_PROGRESS}", Long.toString(minion.progress().progress()));
        placeholders.put(
            "{MINION_PROGRESS_REQUIRED}",
            behavior == null || level >= behavior.config().maxLevel()
                ? "MAX"
                : Long.toString(behavior.config().progressToReach(level + 1))
        );
        placeholders.put("{MINION_STATUS}", minion.active() ? this.config.statusWorking : this.config.statusPaused);
        placeholders.put(
            "{MINION_CHEST}",
            minion.chestPosition() == null ? this.config.chestNotLinkedStatus : this.config.chestLinkedStatus
        );
        placeholders.put(
            "{MINION_MODE}",
            minion.settings().miningMode() == MiningMode.SQUARE ? this.config.modeSquare : this.config.modeLinear
        );
        placeholders.put("{MINION_DIRECTION}", switch (minion.settings().direction()) {
            case SOUTH -> this.config.directionSouth;
            case WEST -> this.config.directionWest;
            case NORTH -> this.config.directionNorth;
            case EAST -> this.config.directionEast;
        });
        placeholders.put("{STORAGE_USED}", Integer.toString(this.countStoredItems(minion)));
        placeholders.put("{STORAGE_CAPACITY}", Integer.toString(minion.storage().capacity()));
        return placeholders;
    }

    private int countStoredItems(Minion minion) {
        int stored = 0;
        for (int slot = 0; slot < minion.storage().capacity(); slot++) {
            if (minion.storage().item(slot) != null) {
                stored++;
            }
        }
        return stored;
    }

    private String format(String input, Map<String, String> placeholders) {
        String formatted = input;
        for (Map.Entry<String, String> placeholder : placeholders.entrySet()) {
            formatted = formatted.replace(placeholder.getKey(), placeholder.getValue());
        }
        return formatted;
    }

    private void refresh(Player player, Minion minion) {
        player.closeInventory();
        this.open(player, minion);
    }

    private void withAccess(
            Player player,
            Minion minion,
            MinionAccessAction action,
            Consumer<Minion> operation
    ) {
        this.access.findAccessible(
                player,
                minion.id(),
                action
        ).ifPresent(operation);
    }

    private void send(Player player, Notice notice) {
        this.notices.create().viewer(player).notice(notice).send();
    }
}
