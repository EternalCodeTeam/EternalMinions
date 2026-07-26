package com.eternalcode.minions.minion;

import com.eternalcode.minions.minion.storage.MinionSettings;
import com.eternalcode.minions.minion.storage.MinionStorage;
import com.eternalcode.minions.minion.upgrade.MinionUpgrades;
import java.util.UUID;

public final class Minion {

    private final MinionId id;
    private final UUID ownerId;
    private final String behaviorId;
    private final MinionPosition position;
    private final boolean active;
    private final MinionProgress progress;
    private final MinionEquipment equipment;
    private final MinionStorage storage;
    private final MinionUpgrades upgrades;
    private final MinionPosition chestPosition;
    private final MinionSettings settings;

    public Minion(
        MinionId id,
        UUID ownerId,
        String behaviorId,
        MinionPosition position,
        boolean active,
        MinionProgress progress,
        MinionEquipment equipment,
        MinionStorage storage,
        MinionUpgrades upgrades,
        MinionPosition chestPosition,
        MinionSettings settings
    ) {
        if (id == null || ownerId == null || position == null) {
            throw new IllegalArgumentException("Minion identity and position are required");
        }
        if (behaviorId == null || behaviorId.isBlank()) {
            throw new IllegalArgumentException("Behavior id must not be blank");
        }
        if (progress == null || equipment == null || storage == null || upgrades == null || settings == null) {
            throw new IllegalArgumentException("Minion progress, equipment, storage, upgrades and settings are required");
        }
        this.id = id;
        this.ownerId = ownerId;
        this.behaviorId = behaviorId;
        this.position = position;
        this.active = active;
        this.progress = progress;
        this.equipment = equipment;
        this.storage = storage;
        this.upgrades = upgrades;
        this.chestPosition = chestPosition;
        this.settings = settings;
    }

    public MinionId id() { return this.id; }
    public UUID ownerId() { return this.ownerId; }
    public String behaviorId() { return this.behaviorId; }
    public MinionPosition position() { return this.position; }
    public boolean active() { return this.active; }
    public MinionProgress progress() { return this.progress; }
    public MinionEquipment equipment() { return this.equipment; }
    public MinionStorage storage() { return this.storage; }
    public MinionUpgrades upgrades() { return this.upgrades; }
    public MinionPosition chestPosition() { return this.chestPosition; }
    public MinionSettings settings() { return this.settings; }

    public Minion withActive(boolean active) {
        return new Minion(this.id, this.ownerId, this.behaviorId, this.position, active,
            this.progress, this.equipment, this.storage, this.upgrades, this.chestPosition, this.settings);
    }

    public Minion withProgress(MinionProgress progress) {
        return new Minion(this.id, this.ownerId, this.behaviorId, this.position, this.active,
            progress, this.equipment, this.storage, this.upgrades, this.chestPosition, this.settings);
    }

    public Minion withEquipment(MinionEquipment equipment) {
        return new Minion(this.id, this.ownerId, this.behaviorId, this.position, this.active,
            this.progress, equipment, this.storage, this.upgrades, this.chestPosition, this.settings);
    }

    public Minion withStorage(MinionStorage storage) {
        return new Minion(this.id, this.ownerId, this.behaviorId, this.position, this.active,
            this.progress, this.equipment, storage, this.upgrades, this.chestPosition, this.settings);
    }

    public Minion withUpgrades(MinionUpgrades upgrades) {
        return new Minion(this.id, this.ownerId, this.behaviorId, this.position, this.active,
            this.progress, this.equipment, this.storage, upgrades, this.chestPosition, this.settings);
    }

    public Minion withChestPosition(MinionPosition chestPosition) {
        return new Minion(this.id, this.ownerId, this.behaviorId, this.position, this.active,
            this.progress, this.equipment, this.storage, this.upgrades, chestPosition, this.settings);
    }

    public Minion withSettings(MinionSettings settings) {
        return new Minion(this.id, this.ownerId, this.behaviorId, this.position, this.active,
            this.progress, this.equipment, this.storage, this.upgrades, this.chestPosition, settings);
    }

    public MinionDetails details() {
        return new MinionDetails(this.id, this.ownerId, this.behaviorId, this.position, this.progress.level(), this.active);
    }
}
