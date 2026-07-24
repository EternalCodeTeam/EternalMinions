package com.eternalcode.minions.gui;

import com.eternalcode.minions.config.MessagesConfig;
import com.eternalcode.minions.config.MinionPanelAction;
import com.eternalcode.minions.config.MinionPanelConfig;
import com.eternalcode.minions.config.MinionPanelElementConfig;
import com.eternalcode.minions.config.MinionPanelLayout;
import com.eternalcode.minions.minion.MiningMode;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionEquipment;
import com.eternalcode.minions.minion.MinionStorage;
import com.eternalcode.minions.minion.MinionType;
import com.eternalcode.minions.minion.MinionTypeService;
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
    private final MinionTypeService types;
    private final PanelItemFactory items;
    private final Consumer<Minion> update;
    private final BiConsumer<Player, Minion> pickup;
    private final BiConsumer<Player, Minion> openUpgrades;
    private final BiConsumer<Player, Minion> linkChest;

    public MinionPanel(
        Plugin plugin,
        MinionPanelConfig config,
        MessagesConfig messages,
        NoticeService notices,
        MiniMessage miniMessage,
        MinionTypeService types,
        Consumer<Minion> update,
        BiConsumer<Player, Minion> pickup,
        BiConsumer<Player, Minion> openUpgrades,
        BiConsumer<Player, Minion> linkChest
    ) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.notices = notices;
        this.miniMessage = miniMessage;
        this.types = types;
        this.items = new PanelItemFactory(miniMessage);
        this.update = update;
        this.pickup = pickup;
        this.openUpgrades = openUpgrades;
        this.linkChest = linkChest;
    }

    public void open(Player player, Minion minion) {
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

        gui.addPane(Slot.fromIndex(0), pane);
        gui.show(player);
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
            case COLLECT_ITEMS -> this.createConfiguredElement(element, placeholders, event -> this.collect(player, minion));
            case PICKUP_MINION -> this.createConfiguredElement(element, placeholders, event -> this.pickup.accept(player, minion));
            case TOGGLE_ACTIVE -> this.createConfiguredElement(element, placeholders, event -> this.toggleActive(player, minion));
            case UPGRADES -> this.createConfiguredElement(element, placeholders, event -> this.openUpgrades.accept(player, minion));
            case LINK_CHEST -> this.createConfiguredElement(element, placeholders, event -> this.linkChest.accept(player, minion));
            case ROTATE -> this.createConfiguredElement(element, placeholders, event -> this.rotate(player, minion));
            case TOGGLE_MODE -> this.createConfiguredElement(element, placeholders, event -> this.toggleMode(player, minion));
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
        return new GuiItem(icon, event -> {
            ItemStack cursor = event.getCursor();
            Minion updated = minion.withEquipment(new MinionEquipment(cursor.getType().isAir() ? null : cursor));
            this.update.accept(updated);
            player.setItemOnCursor(tool);
            this.send(player, this.messages.minionToolUpdated);
            this.refresh(player, updated);
        }, this.plugin);
    }

    private GuiItem createConfiguredElement(
        MinionPanelElementConfig element,
        Map<String, String> placeholders,
        Consumer<InventoryClickEvent> click
    ) {
        ItemStack item = this.items.create(element, placeholders);
        return click == null ? new GuiItem(item, this.plugin) : new GuiItem(item, click, this.plugin);
    }

    private void toggleActive(Player player, Minion minion) {
        Minion updated = minion.withActive(!minion.active());
        this.update.accept(updated);
        this.send(player, updated.active() ? this.messages.minionResumed : this.messages.minionPaused);
        this.refresh(player, updated);
    }

    private void rotate(Player player, Minion minion) {
        Minion updated = minion.withSettings(minion.settings().withDirection(minion.settings().direction().rotated()));
        this.update.accept(updated);
        this.send(player, this.messages.minionRotated);
        this.refresh(player, updated);
    }

    private void toggleMode(Player player, Minion minion) {
        MiningMode mode = minion.settings().miningMode() == MiningMode.SQUARE ? MiningMode.LINEAR : MiningMode.SQUARE;
        Minion updated = minion.withSettings(minion.settings().withMiningMode(mode));
        this.update.accept(updated);
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
            ItemStack remaining = player.getInventory().addItem(item).get(0);
            storage = storage.withItem(slot, remaining);
        }
        Minion updated = minion.withStorage(storage);
        this.update.accept(updated);
        this.send(player, this.messages.minionStorageCollected);
        this.refresh(player, updated);
    }

    private Map<String, String> createPlaceholders(Minion minion) {
        MinionType type = this.types.type(minion.behaviorId()).orElse(null);
        int level = minion.progress().level();

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{MINION_ID}", Long.toString(minion.id().value()));
        placeholders.put("{MINION_BEHAVIOR}", type == null ? minion.behaviorId() : type.displayName());
        placeholders.put("{MINION_LEVEL}", Integer.toString(level));
        placeholders.put("{MINION_MAX_LEVEL}", type == null ? "?" : Integer.toString(type.maxLevel()));
        placeholders.put("{MINION_PROGRESS}", Long.toString(minion.progress().progress()));
        placeholders.put(
            "{MINION_PROGRESS_REQUIRED}",
            type == null || level >= type.maxLevel() ? "MAX" : Long.toString(type.progressToReach(level + 1))
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

    private void send(Player player, Notice notice) {
        this.notices.create().viewer(player).notice(notice).send();
    }
}
