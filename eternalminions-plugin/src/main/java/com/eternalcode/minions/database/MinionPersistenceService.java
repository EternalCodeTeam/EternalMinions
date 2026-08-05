package com.eternalcode.minions.database;

import com.eternalcode.minions.database.repository.MinionEquipmentRepository;
import com.eternalcode.minions.database.repository.MinionRepository;
import com.eternalcode.minions.database.repository.MinionStateRepository;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionId;
import com.eternalcode.minions.minion.MinionPosition;
import com.eternalcode.minions.minion.storage.MinionChestLinkRepository;
import com.eternalcode.minions.minion.storage.MinionSettingsRepository;
import com.eternalcode.minions.minion.storage.MinionStorageRepository;
import com.eternalcode.minions.minion.upgrade.MinionUpgradeRepository;
import com.eternalcode.minions.minion.upgrade.UpgradeKind;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.inventory.ItemStack;

public final class MinionPersistenceService {

    private final Logger logger;
    private final MinionRepository minions;
    private final MinionStateRepository states;
    private final MinionSettingsRepository settings;
    private final MinionEquipmentRepository equipment;
    private final MinionStorageRepository storage;
    private final MinionUpgradeRepository upgrades;
    private final MinionChestLinkRepository chests;

    public MinionPersistenceService(
            Logger logger,
            MinionRepository minions,
            MinionStateRepository states,
            MinionSettingsRepository settings,
            MinionEquipmentRepository equipment,
            MinionStorageRepository storage,
            MinionUpgradeRepository upgrades,
            MinionChestLinkRepository chests
    ) {
        this.logger = logger;
        this.minions = minions;
        this.states = states;
        this.settings = settings;
        this.equipment = equipment;
        this.storage = storage;
        this.upgrades = upgrades;
        this.chests = chests;
    }

    private static byte[] serializeStorageSlot(Minion minion, int slot) {
        if (slot >= minion.storage().capacity()) {
            return new byte[0];
        }
        ItemStack item = minion.storage().item(slot);
        return ItemDataCodec.encode(item);
    }

    public void create(Minion minion) {
        if (!this.minions.ready()) {
            return;
        }
        this.report(this.minions.create(MinionData.capture(minion)), "create minion " + minion.id().value());
    }

    public void saveState(Minion minion) {
        if (!this.minions.ready()) {
            return;
        }
        this.report(
                this.states.saveState(
                        minion.id(),
                        minion.progress().level(),
                        minion.progress().progress(),
                        System.currentTimeMillis()
                ),
                "save state for minion " + minion.id().value()
        );
    }

    public void saveSettings(Minion minion) {
        if (!this.minions.ready()) {
            return;
        }
        this.report(
                this.settings.saveSettings(minion.id(), minion.settings()),
                "save settings for minion " + minion.id().value()
        );
    }

    public void saveEquipment(Minion minion) {
        if (!this.minions.ready()) {
            return;
        }

        byte[] serializedTool = ItemDataCodec.encode(minion.equipment().tool());
        CompletableFuture<Void> operation = serializedTool.length == 0
                ? this.equipment.deleteSlot(minion.id(), MinionEquipmentSlot.TOOL)
                : this.equipment.saveSlot(minion.id(), MinionEquipmentSlot.TOOL, serializedTool);
        this.report(operation, "save equipment for minion " + minion.id().value());
    }

    public void saveStorage(Minion previous, Minion updated) {
        if (!this.minions.ready()) {
            return;
        }

        List<CompletableFuture<Void>> operations = new ArrayList<>();
        int capacity = Math.max(previous.storage().capacity(), updated.storage().capacity());
        for (int slot = 0; slot < capacity; slot++) {
            byte[] previousItem = serializeStorageSlot(previous, slot);
            byte[] updatedItem = serializeStorageSlot(updated, slot);
            if (Arrays.equals(previousItem, updatedItem)) {
                continue;
            }
            CompletableFuture<Void> operation = updatedItem.length == 0
                    ? this.storage.deleteSlot(updated.id(), slot)
                    : this.storage.saveSlot(updated.id(), slot, updatedItem);
            operations.add(operation);
        }
        if (operations.isEmpty()) {
            return;
        }
        this.report(
                CompletableFuture.allOf(operations.toArray(CompletableFuture[]::new)),
                "save storage for minion " + updated.id().value()
        );
    }

    public void saveUpgrade(Minion minion, UpgradeKind upgrade) {
        if (!this.minions.ready()) {
            return;
        }

        int tier = minion.upgrades().tier(upgrade);
        CompletableFuture<Void> operation = tier == 0
                ? this.upgrades.deleteUpgrade(minion.id(), upgrade)
                : this.upgrades.saveUpgrade(minion.id(), upgrade, tier);
        this.report(operation, "save upgrade for minion " + minion.id().value());
    }

    public void saveChestLink(Minion minion) {
        if (!this.minions.ready()) {
            return;
        }

        MinionPosition chest = minion.chestPosition();
        CompletableFuture<Void> operation = chest == null
                ? this.chests.deleteLink(minion.id())
                : this.chests.saveLink(minion.id(), chest);
        this.report(operation, "save chest link for minion " + minion.id().value());
    }

    public void delete(MinionId minionId) {
        if (!this.minions.ready()) {
            return;
        }
        this.report(this.minions.deleteMinion(minionId), "delete minion " + minionId.value());
    }

    private void report(CompletableFuture<Void> operation, String action) {
        operation.exceptionally(error -> {
            this.logger.log(Level.SEVERE, "Unable to " + action, error);
            return null;
        });
    }
}
