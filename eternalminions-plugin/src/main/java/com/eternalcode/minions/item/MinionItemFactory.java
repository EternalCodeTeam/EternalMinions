package com.eternalcode.minions.item;

import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionStorage;
import com.eternalcode.minions.minion.MinionType;
import com.eternalcode.minions.minion.MinionTypeService;
import com.eternalcode.minions.minion.MinionUpgradeKind;
import com.eternalcode.minions.minion.MinionUpgrades;
import com.eternalcode.minions.minion.miner.MiningMode;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class MinionItemFactory {

    private final MinionTypeService types;
    private final MiniMessage miniMessage;
    private final NamespacedKey minionKey;
    private final NamespacedKey behaviorKey;
    private final NamespacedKey levelKey;
    private final NamespacedKey progressKey;
    private final NamespacedKey toolKey;
    private final NamespacedKey storageKey;
    private final NamespacedKey upgradesKey;
    private final NamespacedKey miningModeKey;

    public MinionItemFactory(Plugin plugin, MinionTypeService types, MiniMessage miniMessage) {
        this.types = types;
        this.miniMessage = miniMessage;
        this.minionKey = new NamespacedKey(plugin, "minion");
        this.behaviorKey = new NamespacedKey(plugin, "minion_behavior");
        this.levelKey = new NamespacedKey(plugin, "minion_level");
        this.progressKey = new NamespacedKey(plugin, "minion_progress");
        this.toolKey = new NamespacedKey(plugin, "minion_tool");
        this.storageKey = new NamespacedKey(plugin, "minion_storage");
        this.upgradesKey = new NamespacedKey(plugin, "minion_upgrades");
        this.miningModeKey = new NamespacedKey(plugin, "minion_mining_mode");
    }

    public ItemStack create(MinionType type) {
        ItemStack item = type.headItem();
        ItemMeta meta = item.getItemMeta();
        meta.displayName(this.miniMessage.deserialize(type.displayName()));
        meta.lore(List.of(Component.text("Kliknij blok PPM, aby postawić.", NamedTextColor.DARK_GRAY)));
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(this.minionKey, PersistentDataType.BYTE, (byte) 1);
        data.set(this.behaviorKey, PersistentDataType.STRING, type.id());
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack create(Minion minion) {
        MinionType type = this.types.type(minion.behaviorId()).orElseGet(this.types::defaultType);
        ItemStack item = this.create(type);
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(this.behaviorKey, PersistentDataType.STRING, minion.behaviorId());
        data.set(this.levelKey, PersistentDataType.INTEGER, minion.progress().level());
        data.set(this.progressKey, PersistentDataType.LONG, minion.progress().progress());
        data.set(this.toolKey, PersistentDataType.BYTE_ARRAY, encodeItem(minion.equipment().tool()));
        data.set(this.storageKey, PersistentDataType.BYTE_ARRAY, encodeStorage(minion.storage()));
        data.set(this.upgradesKey, PersistentDataType.STRING, encodeUpgrades(minion.upgrades()));
        data.set(this.miningModeKey, PersistentDataType.STRING, minion.settings().miningMode().name());
        item.setItemMeta(meta);
        return item;
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
        String miningModeData = data.getOrDefault(this.miningModeKey, PersistentDataType.STRING, "");
        return Optional.of(new StoredMinionState(
            behaviorId, level, progress, decodeItem(toolData), decodeStorage(storageData),
            decodeUpgrades(upgradesData), decodeMiningMode(miningModeData)));
    }

    public record StoredMinionState(
        String behaviorId,
        int level,
        long progress,
        ItemStack tool,
        ItemStack[] storage,
        MinionUpgrades upgrades,
        MiningMode miningMode
    ) {
    }

    private static MiningMode decodeMiningMode(String encoded) {
        if (encoded.isEmpty()) {
            return MiningMode.SQUARE;
        }
        try {
            return MiningMode.valueOf(encoded);
        }
        catch (IllegalArgumentException ignored) {
            return MiningMode.SQUARE;
        }
    }

    private static String encodeUpgrades(MinionUpgrades upgrades) {
        StringBuilder encoded = new StringBuilder();
        for (MinionUpgradeKind kind : MinionUpgradeKind.values()) {
            int tier = upgrades.tier(kind);
            if (tier == 0) {
                continue;
            }
            if (!encoded.isEmpty()) {
                encoded.append(',');
            }
            encoded.append(kind.name()).append(':').append(tier);
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
                MinionUpgradeKind kind = MinionUpgradeKind.valueOf(entry.substring(0, separator));
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
