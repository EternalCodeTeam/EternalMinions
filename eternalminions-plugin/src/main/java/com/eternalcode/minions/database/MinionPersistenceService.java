package com.eternalcode.minions.database;

import com.eternalcode.minions.database.repository.MinionEquipmentRepository;
import com.eternalcode.minions.database.repository.MinionActionRepository;
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
    private final MinionActionRepository actions;

    public MinionPersistenceService(
            Logger logger,
            MinionRepository minions,
            MinionStateRepository states,
            MinionSettingsRepository settings,
            MinionEquipmentRepository equipment,
            MinionStorageRepository storage,
            MinionUpgradeRepository upgrades,
            MinionChestLinkRepository chests,
            MinionActionRepository actions
    ) {
        this.logger = logger;
        this.minions = minions;
        this.states = states;
        this.settings = settings;
        this.equipment = equipment;
        this.storage = storage;
        this.upgrades = upgrades;
        this.chests = chests;
        this.actions = actions;
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

    public void saveAction(Minion previous, Minion updated) {
        if (!this.minions.ready()) {
            return;
        }
        if (previous == null || updated == null || !previous.id().equals(updated.id())) {
            throw new IllegalArgumentException("Matching previous and updated minions are required");
        }

        MinionActionUpdate.StateChange stateChange = previous.progress().equals(updated.progress())
                ? null
                : new MinionActionUpdate.StateChange(
                        updated.progress().level(),
                        updated.progress().progress(),
                        System.currentTimeMillis()
                );

        List<MinionActionUpdate.EquipmentChange> equipmentChanges =
                this.equipmentChanges(previous, updated);
        List<MinionActionUpdate.StorageChange> storageChanges =
                this.storageChanges(previous, updated);
        MinionActionUpdate actionUpdate = new MinionActionUpdate(
                updated.id().value(),
                stateChange,
                equipmentChanges,
                storageChanges
        );
        if (actionUpdate.isEmpty()) {
            return;
        }

        this.report(
                this.actions.save(actionUpdate),
                "save action for minion " + updated.id().value()
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
        CompletableFuture<Void> toolOperation = serializedTool.length == 0
                ? this.equipment.deleteSlot(minion.id(), MinionEquipmentSlot.TOOL)
                : this.equipment.saveSlot(minion.id(), MinionEquipmentSlot.TOOL, serializedTool);
        CompletableFuture<Void> damageOperation =
                this.equipment.deleteSlot(minion.id(), MinionEquipmentSlot.TOOL_DAMAGE);
        this.report(
                CompletableFuture.allOf(toolOperation, damageOperation),
                "save equipment for minion " + minion.id().value()
        );
    }

    public void saveEquipmentDamage(Minion minion) {
        if (!this.minions.ready()) {
            return;
        }

        int damage = minion.equipment().toolDamage();
        if (damage < 0) {
            return;
        }

        this.report(
                this.equipment.saveSlot(
                        minion.id(),
                        MinionEquipmentSlot.TOOL_DAMAGE,
                        ItemDataCodec.encodeInteger(damage)
                ),
                "save equipment damage for minion " + minion.id().value()
        );
    }

    public void saveStorage(Minion previous, Minion updated) {
        if (!this.minions.ready()) {
            return;
        }

        int capacity = Math.max(previous.storage().capacity(), updated.storage().capacity());
        long changedSlots = 0L;
        for (int slot = 0; slot < capacity; slot++) {
            if (previous.storage().hasSameItem(updated.storage(), slot)) {
                continue;
            }

            changedSlots |= 1L << slot;
        }
        if (changedSlots == 0L) {
            return;
        }

        this.flushStorage(updated, changedSlots);
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

    private List<MinionActionUpdate.EquipmentChange> equipmentChanges(Minion previous, Minion updated) {
        if (previous.equipment() == updated.equipment()) {
            return List.of();
        }
        if (updated.equipment().hasVisualChangeSince(previous.equipment())) {
            return List.of(
                    new MinionActionUpdate.EquipmentChange(
                            MinionEquipmentSlot.TOOL,
                            ItemDataCodec.encode(updated.equipment().tool())
                    ),
                    new MinionActionUpdate.EquipmentChange(
                            MinionEquipmentSlot.TOOL_DAMAGE,
                            new byte[0]
                    )
            );
        }

        int damage = updated.equipment().toolDamage();
        if (damage < 0) {
            return List.of();
        }
        return List.of(new MinionActionUpdate.EquipmentChange(
                MinionEquipmentSlot.TOOL_DAMAGE,
                ItemDataCodec.encodeInteger(damage)
        ));
    }

    private List<MinionActionUpdate.StorageChange> storageChanges(Minion previous, Minion updated) {
        if (previous.storage() == updated.storage()) {
            return List.of();
        }

        int capacity = Math.max(previous.storage().capacity(), updated.storage().capacity());
        List<MinionActionUpdate.StorageChange> changes = new ArrayList<>();
        for (int slot = 0; slot < capacity; slot++) {
            if (previous.storage().hasSameItem(updated.storage(), slot)) {
                continue;
            }
            changes.add(new MinionActionUpdate.StorageChange(
                    slot,
                    serializeStorageSlot(updated, slot)
            ));
        }
        return List.copyOf(changes);
    }

    private void flushStorage(Minion minion, long changedSlots) {
        List<CompletableFuture<Void>> operations = new ArrayList<>(Long.bitCount(changedSlots));
        while (changedSlots != 0L) {
            int slot = Long.numberOfTrailingZeros(changedSlots);
            changedSlots &= changedSlots - 1L;

            byte[] serializedItem = serializeStorageSlot(minion, slot);
            CompletableFuture<Void> operation = serializedItem.length == 0
                    ? this.storage.deleteSlot(minion.id(), slot)
                    : this.storage.saveSlot(minion.id(), slot, serializedItem);
            operations.add(operation);
        }

        this.report(
                CompletableFuture.allOf(operations.toArray(CompletableFuture[]::new)),
                "save storage for minion " + minion.id().value()
        );
    }

}
