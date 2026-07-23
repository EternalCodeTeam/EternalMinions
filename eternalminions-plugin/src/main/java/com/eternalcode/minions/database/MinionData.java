package com.eternalcode.minions.database;

import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionEquipment;
import com.eternalcode.minions.minion.MinionId;
import com.eternalcode.minions.minion.MinionPosition;
import com.eternalcode.minions.minion.MinionProgress;
import com.eternalcode.minions.minion.MinionStorage;
import java.util.ArrayList;
import java.util.List;
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
    byte[] serializedTool,
    List<StoredItemData> storageItems
) {

    public MinionData {
        serializedTool = serializedTool.clone();
        storageItems = List.copyOf(storageItems);
    }

    @Override
    public byte[] serializedTool() {
        return this.serializedTool.clone();
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
        return new MinionData(
            minion.id().value(), minion.ownerId(), minion.behaviorId(), position.worldKey(),
            position.blockX(), position.blockY(), position.blockZ(), minion.active(), minion.progress().level(),
            ItemDataCodec.encode(minion.equipment().tool()), storageItems
        );
    }

    public Minion restore() {
        MinionStorage storage = new MinionStorage(9);
        for (StoredItemData storedItem : this.storageItems) {
            if (storedItem.slot() < storage.capacity()) {
                storage = storage.withItem(storedItem.slot(), ItemDataCodec.decode(storedItem.serializedItem()));
            }
        }
        return new Minion(
            new MinionId(this.id), this.ownerId, this.behaviorId,
            new MinionPosition(this.worldKey, this.blockX, this.blockY, this.blockZ), this.active,
            new MinionProgress(this.level), new MinionEquipment(ItemDataCodec.decode(this.serializedTool)), storage
        );
    }
}
