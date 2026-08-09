package com.eternalcode.minions.minion;

import com.eternalcode.minions.minion.status.MinionStatus;
import com.eternalcode.minions.minion.storage.MinionSettings;
import com.eternalcode.minions.minion.storage.MinionStorage;
import com.eternalcode.minions.minion.upgrade.MinionUpgrades;
import com.eternalcode.minions.minion.upgrade.UpgradeKind;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

public record Minion(
        MinionId id,
        UUID ownerId,
        String behaviorId,
        MinionPosition position,
        MinionProgress progress,
        MinionEquipment equipment,
        MinionStorage storage,
        MinionUpgrades upgrades,
        MinionPosition chestPosition,
        MinionSettings settings
) {

    public Minion {
        if (id == null || ownerId == null || position == null) {
            throw new IllegalArgumentException("Minion identity and position are required");
        }
        if (behaviorId == null || behaviorId.isBlank()) {
            throw new IllegalArgumentException("Behavior id must not be blank");
        }
        if (progress == null || equipment == null || storage == null || upgrades == null || settings == null) {
            throw new IllegalArgumentException("Minion progress, equipment, storage, upgrades and settings are required");
        }
    }

    public Minion withProgress(MinionProgress progress) {
        return new Minion(
                this.id, this.ownerId, this.behaviorId, this.position,
                progress, this.equipment, this.storage, this.upgrades, this.chestPosition, this.settings);
    }

    public Minion withEquipment(MinionEquipment equipment) {
        if (this.equipment == equipment) {
            return this;
        }

        return new Minion(
                this.id, this.ownerId, this.behaviorId, this.position,
                this.progress, equipment, this.storage, this.upgrades, this.chestPosition, this.settings);
    }

    public Minion withStorage(MinionStorage storage) {
        return new Minion(
                this.id, this.ownerId, this.behaviorId, this.position,
                this.progress, this.equipment, storage, this.upgrades, this.chestPosition, this.settings);
    }

    public Minion withUpgrades(MinionUpgrades upgrades) {
        return new Minion(
                this.id, this.ownerId, this.behaviorId, this.position,
                this.progress, this.equipment, this.storage, upgrades, this.chestPosition, this.settings);
    }

    public Minion withChestPosition(MinionPosition chestPosition) {
        return new Minion(
                this.id, this.ownerId, this.behaviorId, this.position,
                this.progress, this.equipment, this.storage, this.upgrades, chestPosition, this.settings);
    }

    public Minion withSettings(MinionSettings settings) {
        return new Minion(
                this.id, this.ownerId, this.behaviorId, this.position,
                this.progress, this.equipment, this.storage, this.upgrades, this.chestPosition, settings);
    }

    public MinionDetails details() {
        return new MinionDetails(this.id, this.ownerId, this.behaviorId, this.position, this.progress.level());
    }

    public MinionSnapshot snapshot(MinionStatus status) {
        Map<String, Integer> upgradeTiers = new LinkedHashMap<>();
        for (Entry<UpgradeKind, Integer> entry : this.upgrades.entries().entrySet()) {
            upgradeTiers.put(entry.getKey().key(), entry.getValue());
        }

        return new MinionSnapshot(
                this.details(),
                this.progress.progress(),
                this.settings.direction(),
                this.equipment.tool(),
                Arrays.asList(this.storage.snapshot()),
                this.storage.capacity(),
                upgradeTiers,
                this.chestPosition,
                status.key()
        );
    }
}
