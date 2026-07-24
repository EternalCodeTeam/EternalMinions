package com.eternalcode.minions.database;

import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionEquipment;
import com.eternalcode.minions.minion.MinionId;
import com.eternalcode.minions.minion.MinionPosition;
import com.eternalcode.minions.minion.MinionProgress;
import com.eternalcode.minions.minion.MinionSettings;
import com.eternalcode.minions.minion.MinionStorage;
import com.eternalcode.minions.minion.MinionUpgradeKind;
import com.eternalcode.minions.minion.MinionUpgrades;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;

public record MinionData(
    long id,
    UUID ownerId,
    String behaviorId,
    String worldKey,
    int blockX,
    int blockY,
    int blockZ,
    boolean active,
    int level,
    long progress,
    byte[] serializedTool,
    List<StoredItemData> storageItems,
    Map<String, Integer> upgrades,
    ChestPositionData chestPosition,
    MinionSettings settings
) {

    public MinionData {
        serializedTool = serializedTool.clone();
        storageItems = List.copyOf(storageItems);
        upgrades = Map.copyOf(upgrades);
        settings = settings == null ? MinionSettings.defaults() : settings;
    }

    @Override
    public byte[] serializedTool() {
        return this.serializedTool.clone();
    }

    public record ChestPositionData(String worldKey, int blockX, int blockY, int blockZ) {
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
        for (MinionUpgradeKind kind : MinionUpgradeKind.values()) {
            int tier = minion.upgrades().tier(kind);
            if (tier > 0) {
                upgrades.put(kind.name(), tier);
            }
        }

        MinionPosition chest = minion.chestPosition();
        ChestPositionData chestPosition = chest == null
            ? null
            : new ChestPositionData(chest.worldKey(), chest.blockX(), chest.blockY(), chest.blockZ());

        return new MinionData(
            minion.id().value(), minion.ownerId(), minion.behaviorId(), position.worldKey(),
            position.blockX(), position.blockY(), position.blockZ(), minion.active(), minion.progress().level(),
            minion.progress().progress(), ItemDataCodec.encode(minion.equipment().tool()), storageItems,
            upgrades, chestPosition, minion.settings()
        );
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
                minionUpgrades = minionUpgrades.withTier(MinionUpgradeKind.valueOf(entry.getKey()), entry.getValue());
            }
            catch (IllegalArgumentException ignored) {
                // An upgrade kind removed from the plugin should not prevent the minion from loading.
            }
        }

        return new Minion(
            new MinionId(this.id), this.ownerId, this.behaviorId,
            new MinionPosition(this.worldKey, this.blockX, this.blockY, this.blockZ), this.active,
            new MinionProgress(this.level, this.progress),
            new MinionEquipment(ItemDataCodec.decode(this.serializedTool)), storage, minionUpgrades,
            this.chestPosition == null ? null : new MinionPosition(
                this.chestPosition.worldKey(),
                this.chestPosition.blockX(),
                this.chestPosition.blockY(),
                this.chestPosition.blockZ()
            ),
            this.settings
        );
    }
}
