package com.eternalcode.minions.minion;

import com.eternalcode.minions.event.MinionCreatedEvent;
import com.eternalcode.minions.event.MinionEventCause;
import com.eternalcode.minions.event.EventDispatcher;
import com.eternalcode.minions.event.MinionPreCreateEvent;
import com.eternalcode.minions.event.MinionPreRemoveEvent;
import com.eternalcode.minions.event.MinionRemovedEvent;
import com.eternalcode.minions.event.MinionUpdatedEvent;
import com.eternalcode.minions.event.MinionUpdateType;
import com.eternalcode.minions.database.MinionData;
import com.eternalcode.minions.database.MinionPersistenceService;
import com.eternalcode.minions.minion.behavior.MinionBehavior;
import com.eternalcode.minions.minion.behavior.MinionBehaviorRegistry;
import com.eternalcode.minions.minion.schedule.MinionScheduler;
import com.eternalcode.minions.minion.status.MinionStatusTracker;
import com.eternalcode.minions.minion.upgrade.UpgradeKind;
import com.eternalcode.minions.render.MinionRenderService;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public final class MinionLifecycleService {

    private final MinionRegistry minions;
    private final MinionScheduler scheduler;
    private final MinionRenderService renders;
    private final MinionPersistenceService persistence;
    private final MinionBehaviorRegistry behaviors;
    private final MinionStatusTracker statuses;
    private final EventDispatcher events;

    public MinionLifecycleService(
        MinionRegistry minions,
        MinionScheduler scheduler,
        MinionRenderService renders,
        MinionPersistenceService persistence,
        MinionBehaviorRegistry behaviors,
        MinionStatusTracker statuses,
        EventDispatcher events
    ) {
        this.minions = minions;
        this.scheduler = scheduler;
        this.renders = renders;
        this.persistence = persistence;
        this.behaviors = behaviors;
        this.statuses = statuses;
        this.events = events;
    }

    public int restoreAll(List<MinionData> loaded) {
        for (MinionData data : loaded) {
            Minion minion = data.restore();
            MinionBehavior behavior = this.behaviors.find(minion.behaviorId()).orElse(null);
            if (behavior != null && behavior.storageCapacity(minion) > minion.storage().capacity()) {
                minion = minion.withStorage(minion.storage().resized(behavior.storageCapacity(minion)));
            }
            this.minions.register(minion);
            this.scheduler.add(minion);
            this.renders.showToNearby(minion);
            this.events.fire(new MinionCreatedEvent(
                this.snapshot(minion),
                MinionEventCause.RESTORE,
                null
            ));
        }
        return loaded.size();
    }

    public boolean add(Minion minion, MinionEventCause cause, UUID actorId) {
        MinionPreCreateEvent event = this.events.fire(new MinionPreCreateEvent(
            this.createRequest(minion),
            cause,
            actorId
        ));
        if (event.isCancelled()) {
            return false;
        }

        this.minions.register(minion);
        this.scheduler.add(minion);
        this.renders.showToNearby(minion);
        this.persistence.create(minion);
        this.events.fire(new MinionCreatedEvent(this.snapshot(minion), cause, actorId));
        return true;
    }

    public boolean remove(MinionId minionId, MinionEventCause cause, UUID actorId) {
        return this.remove(minionId, cause, actorId, ignored -> {});
    }

    boolean remove(
        MinionId minionId,
        MinionEventCause cause,
        UUID actorId,
        Consumer<Minion> afterRemoval
    ) {
        Minion minion = this.minions.findMinion(minionId).orElse(null);
        if (minion == null) {
            return false;
        }

        MinionSnapshot snapshot = this.snapshot(minion);
        MinionPreRemoveEvent event = this.events.fire(new MinionPreRemoveEvent(snapshot, cause, actorId));
        if (event.isCancelled() || this.minions.remove(minionId).isEmpty()) {
            return false;
        }

        this.scheduler.remove(minion);
        this.renders.remove(minion);
        this.statuses.clearStatus(minionId);
        afterRemoval.accept(minion);
        this.persistence.delete(minionId);
        this.events.fire(new MinionRemovedEvent(snapshot, cause, actorId));
        return true;
    }

    public void updateState(Minion minion, MinionEventCause cause, UUID actorId) {
        Minion previous = this.minions.replace(minion);
        this.persistence.saveState(minion);
        this.fireUpdate(previous, minion, MinionUpdateType.PROGRESS, cause, actorId);
    }

    public void updateEquipment(Minion minion) {
        this.updateEquipment(minion, MinionEventCause.INTERNAL, null);
    }

    public void updateEquipment(Minion minion, MinionEventCause cause, UUID actorId) {
        Minion previous = this.minions.replace(minion);
        this.persistence.saveEquipment(minion);
        this.renders.refreshEquipment(minion);
        this.fireUpdate(previous, minion, MinionUpdateType.TOOL, cause, actorId);
    }

    public void updateStorage(Minion minion) {
        this.updateStorage(minion, MinionEventCause.INTERNAL, null);
    }

    public void updateStorage(Minion minion, MinionEventCause cause, UUID actorId) {
        Minion previous = this.minions.findMinion(minion.id()).orElse(null);
        if (previous == null) {
            return;
        }
        this.minions.replace(minion);
        this.persistence.saveStorage(previous, minion);
        this.fireUpdate(previous, minion, MinionUpdateType.STORAGE, cause, actorId);
    }

    public void updateSettings(Minion minion) {
        this.updateSettings(minion, MinionEventCause.INTERNAL, null);
    }

    public void updateSettings(Minion minion, MinionEventCause cause, UUID actorId) {
        Minion previous = this.minions.replace(minion);
        this.persistence.saveSettings(minion);
        this.renders.refreshRotation(minion);
        this.fireUpdate(previous, minion, MinionUpdateType.DIRECTION, cause, actorId);
    }

    public void updateUpgrade(Minion minion, UpgradeKind upgrade, MinionEventCause cause, UUID actorId) {
        Minion previous = this.minions.findMinion(minion.id()).orElse(null);
        if (previous == null) {
            return;
        }
        this.minions.replace(minion);
        this.persistence.saveUpgrade(minion, upgrade);
        this.persistence.saveStorage(previous, minion);
        this.fireUpdate(previous, minion, MinionUpdateType.UPGRADE, cause, actorId);
    }

    public void updateChestLink(Minion minion) {
        this.updateChestLink(minion, MinionEventCause.INTERNAL, null);
    }

    public void updateChestLink(Minion minion, MinionEventCause cause, UUID actorId) {
        Minion previous = this.minions.replace(minion);
        this.persistence.saveChestLink(minion);
        this.fireUpdate(previous, minion, MinionUpdateType.CHEST_LINK, cause, actorId);
    }

    private MinionCreateRequest createRequest(Minion minion) {
        return new MinionCreateRequest(
            minion.ownerId(),
            minion.behaviorId(),
            minion.position(),
            minion.settings().direction()
        );
    }

    private void fireUpdate(
        Minion previous,
        Minion current,
        MinionUpdateType updateType,
        MinionEventCause cause,
        UUID actorId
    ) {
        this.events.fire(new MinionUpdatedEvent(
            this.snapshot(previous),
            this.snapshot(current),
            updateType,
            cause,
            actorId
        ));
    }

    private MinionSnapshot snapshot(Minion minion) {
        return minion.snapshot(this.statuses.status(minion.id()));
    }

}
