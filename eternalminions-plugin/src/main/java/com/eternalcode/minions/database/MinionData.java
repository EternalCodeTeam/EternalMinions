package com.eternalcode.minions.database;

import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionEquipment;
import com.eternalcode.minions.minion.MinionId;
import com.eternalcode.minions.minion.MinionPosition;
import com.eternalcode.minions.minion.MinionProgress;
import com.eternalcode.minions.minion.storage.MinionSettings;
import com.eternalcode.minions.minion.storage.MinionStorage;
import com.eternalcode.minions.minion.upgrade.MinionUpgrades;
import com.eternalcode.minions.minion.upgrade.UpgradeKind;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

public record MinionData(
        long id,
        UUID ownerId,
        String behaviorId,
        String worldKey,
        int blockX,
        int blockY,
        int blockZ,
        int level,
        long progress,
        byte[] serializedTool,
        int toolDamage,
        List<StoredItemData> storageItems,
        Map<String, Integer> upgrades,
        ChestPositionData chestPosition,
        MinionSettings settings,
        long createdAt
) {

    public MinionData {
        if (ownerId == null || behaviorId == null || behaviorId.isBlank() || worldKey == null || worldKey.isBlank()) {
            throw new IllegalArgumentException("Minion owner, behavior and world are required");
        }
        if (serializedTool == null || storageItems == null || upgrades == null) {
            throw new IllegalArgumentException("Minion equipment, storage and upgrades are required");
        }
        if (id < 0L) {
            throw new IllegalArgumentException("Minion id cannot be negative");
        }
        if (level < 0) {
            throw new IllegalArgumentException("Minion level cannot be negative");
        }
        if (progress < 0L) {
            throw new IllegalArgumentException("Minion progress cannot be negative");
        }
        if (toolDamage < -1) {
            throw new IllegalArgumentException("Tool damage cannot be lower than -1");
        }
        if (createdAt < 0L) {
            throw new IllegalArgumentException("Minion creation time cannot be negative");
        }
        serializedTool = serializedTool.clone();
        storageItems = List.copyOf(storageItems);
        upgrades = Map.copyOf(upgrades);
        settings = settings == null ? MinionSettings.defaults() : settings;
    }

    public MinionData(
            long id,
            UUID ownerId,
            String behaviorId,
            String worldKey,
            int blockX,
            int blockY,
            int blockZ,
            int level,
            long progress,
            byte[] serializedTool,
            List<StoredItemData> storageItems,
            Map<String, Integer> upgrades,
            ChestPositionData chestPosition,
            MinionSettings settings
    ) {
        this(
                id, ownerId, behaviorId, worldKey, blockX, blockY, blockZ, level, progress,
                serializedTool, -1, storageItems, upgrades, chestPosition, settings, System.currentTimeMillis()
        );
    }

    public static MinionData capture(Minion minion) {
        MinionPosition position = minion.position();
        List<StoredItemData> storageItems = new ArrayList<>();
        for (int slot = 0; slot < minion.storage().capacity(); slot++) {
            ItemStack item = minion.storage().item(slot);
            if (item != null) {
                storageItems.add(new StoredItemData(slot, ItemDataCodec.encode(item)));
            }
        }

        Map<String, Integer> upgrades = new LinkedHashMap<>();
        for (Map.Entry<UpgradeKind, Integer> entry : minion.upgrades().entries().entrySet()) {
            upgrades.put(entry.getKey().key(), entry.getValue());
        }

        MinionPosition chest = minion.chestPosition();
        ChestPositionData chestPosition = chest == null
                ? null
                : new ChestPositionData(chest.worldKey(), chest.blockX(), chest.blockY(), chest.blockZ());

        return new MinionData(
                minion.id().value(), minion.ownerId(), minion.behaviorId(), position.worldKey(),
                position.blockX(), position.blockY(), position.blockZ(), minion.progress().level(),
                minion.progress().progress(), ItemDataCodec.encode(minion.equipment().tool()), -1, storageItems,
                upgrades, chestPosition, minion.settings(), System.currentTimeMillis()
        );
    }

    @Override
    public byte[] serializedTool() {
        return this.serializedTool.clone();
    }

    public Minion restore() {
        int capacity = 9;
        for (StoredItemData storedItem : this.storageItems) {
            capacity = Math.max(capacity, storedItem.slot() + 1);
        }
        MinionStorage storage = new MinionStorage(capacity);
        for (StoredItemData storedItem : this.storageItems) {
            storage = storage.withItem(storedItem.slot(), ItemDataCodec.decode(storedItem.serializedItem()));
        }

        MinionUpgrades minionUpgrades = MinionUpgrades.none();
        for (Map.Entry<String, Integer> entry : this.upgrades.entrySet()) {
            try {
                minionUpgrades = minionUpgrades.withTier(new UpgradeKind(entry.getKey()), entry.getValue());
            }
            catch (IllegalArgumentException ignored) {
                // An upgrade kind removed from the plugin should not prevent the minion from loading.
            }
        }

        ItemStack tool = ItemDataCodec.decode(this.serializedTool);
        if (tool != null && this.toolDamage >= 0) {
            ItemMeta itemMeta = tool.getItemMeta();
            if (itemMeta instanceof Damageable damageable) {
                damageable.setDamage(this.toolDamage);
                tool.setItemMeta(itemMeta);
            }
        }

        return new Minion(
                new MinionId(this.id), this.ownerId, this.behaviorId,
                new MinionPosition(this.worldKey, this.blockX, this.blockY, this.blockZ),
                new MinionProgress(this.level, this.progress),
                new MinionEquipment(tool), storage, minionUpgrades,
                this.chestPosition == null ? null : new MinionPosition(
                        this.chestPosition.worldKey(),
                        this.chestPosition.blockX(),
                        this.chestPosition.blockY(),
                        this.chestPosition.blockZ()
                ),
                this.settings
        );
    }

    public record ChestPositionData(String worldKey, int blockX, int blockY, int blockZ) {
    }
}
