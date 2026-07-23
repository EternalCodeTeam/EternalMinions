package com.eternalcode.minions.minion;

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

    public Minion(
        MinionId id,
        UUID ownerId,
        String behaviorId,
        MinionPosition position,
        boolean active,
        MinionProgress progress,
        MinionEquipment equipment,
        MinionStorage storage
    ) {
        if (id == null || ownerId == null || position == null) {
            throw new IllegalArgumentException("Minion identity and position are required");
        }
        if (behaviorId == null || behaviorId.isBlank()) {
            throw new IllegalArgumentException("Behavior id must not be blank");
        }
        if (progress == null || equipment == null || storage == null) {
            throw new IllegalArgumentException("Minion progress, equipment and storage are required");
        }
        this.id = id;
        this.ownerId = ownerId;
        this.behaviorId = behaviorId;
        this.position = position;
        this.active = active;
        this.progress = progress;
        this.equipment = equipment;
        this.storage = storage;
    }

    public MinionId id() { return this.id; }
    public UUID ownerId() { return this.ownerId; }
    public String behaviorId() { return this.behaviorId; }
    public MinionPosition position() { return this.position; }
    public boolean active() { return this.active; }
    public MinionProgress progress() { return this.progress; }
    public MinionEquipment equipment() { return this.equipment; }
    public MinionStorage storage() { return this.storage; }

    public Minion withActive(boolean active) {
        return new Minion(this.id, this.ownerId, this.behaviorId, this.position, active,
            this.progress, this.equipment, this.storage);
    }

    public Minion withProgress(MinionProgress progress) {
        return new Minion(this.id, this.ownerId, this.behaviorId, this.position, this.active,
            progress, this.equipment, this.storage);
    }

    public Minion withEquipment(MinionEquipment equipment) {
        return new Minion(this.id, this.ownerId, this.behaviorId, this.position, this.active,
            this.progress, equipment, this.storage);
    }

    public Minion withStorage(MinionStorage storage) {
        return new Minion(this.id, this.ownerId, this.behaviorId, this.position, this.active,
            this.progress, this.equipment, storage);
    }

    public MinionDetails details() {
        return new MinionDetails(this.id, this.ownerId, this.behaviorId, this.position, this.progress.level(), this.active);
    }
}
