package com.eternalcode.minions.item;

import com.eternalcode.minions.config.AbstractMinionConfig;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionBehavior;
import com.eternalcode.minions.minion.MinionBehaviorRegistry;
import com.eternalcode.minions.minion.storage.MinionStorage;
import com.eternalcode.minions.minion.upgrade.MinionUpgrades;
import com.eternalcode.minions.minion.upgrade.UpgradeKind;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class MinionItemFactory {

    private final MinionBehaviorRegistry behaviors;
    private final MinionAppearanceItems appearance;
    private final MiniMessage miniMessage;
    private final NamespacedKey minionKey;
    private final NamespacedKey behaviorKey;
    private final NamespacedKey levelKey;
    private final NamespacedKey progressKey;
    private final NamespacedKey toolKey;
    private final NamespacedKey storageKey;
    private final NamespacedKey upgradesKey;

    public MinionItemFactory(
        Plugin plugin,
        MinionBehaviorRegistry behaviors,
        MinionAppearanceItems appearance,
        MiniMessage miniMessage
    ) {
        this.behaviors = behaviors;
        this.appearance = appearance;
        this.miniMessage = miniMessage;
        this.minionKey = new NamespacedKey(plugin, "minion");
        this.behaviorKey = new NamespacedKey(plugin, "minion_behavior");
        this.levelKey = new NamespacedKey(plugin, "minion_level");
        this.progressKey = new NamespacedKey(plugin, "minion_progress");
        this.toolKey = new NamespacedKey(plugin, "minion_tool");
        this.storageKey = new NamespacedKey(plugin, "minion_storage");
        this.upgradesKey = new NamespacedKey(plugin, "minion_upgrades");
    }

    public ItemStack create(MinionBehavior behavior) {
        ItemStack item = this.appearance.head(behavior.config());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(this.miniMessage.deserialize(behavior.config().displayName));
        meta.lore(List.of(Component.text("Kliknij blok PPM, aby postawić.", NamedTextColor.DARK_GRAY)));
        this.applyPresentation(
            meta,
            behavior.config(),
            1,
            0L,
            0,
            behavior.config().storageCapacity(MinionUpgrades.none())
        );
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(this.minionKey, PersistentDataType.BYTE, (byte) 1);
        data.set(this.behaviorKey, PersistentDataType.STRING, behavior.id());
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack create(Minion minion) {
        MinionBehavior behavior = this.behaviors.find(minion.behaviorId())
            .orElseGet(this.behaviors::defaultBehavior);
        ItemStack item = this.create(behavior);
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(this.behaviorKey, PersistentDataType.STRING, minion.behaviorId());
        data.set(this.levelKey, PersistentDataType.INTEGER, minion.progress().level());
        data.set(this.progressKey, PersistentDataType.LONG, minion.progress().progress());
        data.set(this.toolKey, PersistentDataType.BYTE_ARRAY, encodeItem(minion.equipment().tool()));
        data.set(this.storageKey, PersistentDataType.BYTE_ARRAY, encodeStorage(minion.storage()));
        data.set(this.upgradesKey, PersistentDataType.STRING, encodeUpgrades(minion.upgrades()));
        this.applyPresentation(
            meta,
            behavior.config(),
            minion.progress().level(),
            minion.progress().progress(),
            this.countStoredItems(minion.storage()),
            behavior.storageCapacity(minion)
        );
        item.setItemMeta(meta);
        return item;
    }

    private void applyPresentation(
        ItemMeta meta,
        AbstractMinionConfig config,
        int level,
        long progress,
        int storedItems,
        int storageCapacity
    ) {
        meta.displayName(this.render(config.displayName));
        meta.lore(this.createLore(config, level, progress, storedItems, storageCapacity));
    }

    private List<Component> createLore(
        AbstractMinionConfig config,
        int level,
        long progress,
        int storedItems,
        int storageCapacity
    ) {
        List<Component> lore = new ArrayList<>(config.itemLore.size());
        int maxLevel = config.maxLevel();
        long requiredProgress = level >= maxLevel ? 0L : config.progressToReach(level + 1);

        for (String line : config.itemLore) {
            lore.add(this.render(this.formatItemLore(
                line,
                level,
                maxLevel,
                progress,
                requiredProgress,
                storedItems,
                storageCapacity,
                config.maximumProgressText
            )));
        }

        return lore;
    }

    private String formatItemLore(
        String input,
        int level,
        int maxLevel,
        long progress,
        long requiredProgress,
        int storedItems,
        int storageCapacity,
        String maximumProgressText
    ) {
        String progressRequired = level >= maxLevel ? maximumProgressText : Long.toString(requiredProgress);

        return input
            .replace("{MINION_LEVEL}", Integer.toString(level))
            .replace("{MINION_MAX_LEVEL}", Integer.toString(maxLevel))
            .replace("{MINION_PROGRESS}", Long.toString(progress))
            .replace("{MINION_PROGRESS_REQUIRED}", progressRequired)
            .replace("{STORAGE_USED}", Integer.toString(storedItems))
            .replace("{STORAGE_CAPACITY}", Integer.toString(storageCapacity));
    }

    private Component render(String input) {
        return this.miniMessage.deserialize(input)
            .decoration(TextDecoration.ITALIC, false);
    }

    private int countStoredItems(MinionStorage storage) {
        int storedItems = 0;

        for (ItemStack item : storage.snapshot()) {
            if (item != null) {
                storedItems += item.getAmount();
            }
        }

        return storedItems;
    }

    public boolean isMinion(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        Byte value = item.getPersistentDataContainer().get(this.minionKey, PersistentDataType.BYTE);
        return value != null && value == 1;
    }

    public Optional<StoredMinionState> readState(ItemStack item) {
        if (!this.isMinion(item)) {
            return Optional.empty();
        }

        PersistentDataContainer data = item.getItemMeta().getPersistentDataContainer();
        String behaviorId = data.get(this.behaviorKey, PersistentDataType.STRING);
        if (behaviorId == null) {
            return Optional.empty();
        }

        int level = data.getOrDefault(this.levelKey, PersistentDataType.INTEGER, 1);
        long progress = data.getOrDefault(this.progressKey, PersistentDataType.LONG, 0L);
        byte[] toolData = data.getOrDefault(this.toolKey, PersistentDataType.BYTE_ARRAY, new byte[0]);
        byte[] storageData = data.getOrDefault(this.storageKey, PersistentDataType.BYTE_ARRAY, new byte[0]);
        String upgradesData = data.getOrDefault(this.upgradesKey, PersistentDataType.STRING, "");
        return Optional.of(new StoredMinionState(
            behaviorId, level, progress, decodeItem(toolData), decodeStorage(storageData),
            decodeUpgrades(upgradesData)));
    }

    public record StoredMinionState(
        String behaviorId,
        int level,
        long progress,
        ItemStack tool,
        ItemStack[] storage,
        MinionUpgrades upgrades
    ) {
    }

    private static String encodeUpgrades(MinionUpgrades upgrades) {
        StringBuilder encoded = new StringBuilder();
        for (Map.Entry<UpgradeKind, Integer> entry : upgrades.entries().entrySet()) {
            if (!encoded.isEmpty()) {
                encoded.append(',');
            }
            encoded.append(entry.getKey().key()).append(':').append(entry.getValue());
        }
        return encoded.toString();
    }

    private static MinionUpgrades decodeUpgrades(String encoded) {
        MinionUpgrades upgrades = MinionUpgrades.none();
        if (encoded.isEmpty()) {
            return upgrades;
        }
        for (String entry : encoded.split(",")) {
            int separator = entry.indexOf(':');
            if (separator < 1) {
                continue;
            }
            try {
                UpgradeKind kind = new UpgradeKind(entry.substring(0, separator));
                upgrades = upgrades.withTier(kind, Integer.parseInt(entry.substring(separator + 1)));
            }
            catch (IllegalArgumentException ignored) {
                // Unknown kinds or malformed tiers in old items should not block placement.
            }
        }
        return upgrades;
    }

    private static byte[] encodeItem(ItemStack item) {
        return item == null ? new byte[0] : item.serializeAsBytes();
    }

    private static ItemStack decodeItem(byte[] data) {
        return data == null || data.length == 0 ? null : ItemStack.deserializeBytes(data);
    }

    private static byte[] encodeStorage(MinionStorage storage) {
        ItemStack[] items = storage.snapshot();
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream();
             DataOutputStream output = new DataOutputStream(buffer)) {
            output.writeInt(items.length);
            for (ItemStack item : items) {
                byte[] encoded = encodeItem(item);
                output.writeInt(encoded.length);
                output.write(encoded);
            }
            return buffer.toByteArray();
        }
        catch (IOException exception) {
            throw new IllegalStateException("Unable to serialize minion storage", exception);
        }
    }

    private static ItemStack[] decodeStorage(byte[] data) {
        if (data == null || data.length == 0) {
            return new ItemStack[0];
        }
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(data))) {
            ItemStack[] items = new ItemStack[input.readInt()];
            for (int slot = 0; slot < items.length; slot++) {
                byte[] encoded = new byte[input.readInt()];
                input.readFully(encoded);
                items[slot] = decodeItem(encoded);
            }
            return items;
        }
        catch (IOException exception) {
            throw new IllegalStateException("Unable to deserialize minion storage", exception);
        }
    }
}
