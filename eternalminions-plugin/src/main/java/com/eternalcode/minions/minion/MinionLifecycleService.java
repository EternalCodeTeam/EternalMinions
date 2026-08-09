package com.eternalcode.minions.minion;

import com.eternalcode.minions.access.MinionAccessAction;
import com.eternalcode.minions.event.MinionCreatedEvent;
import com.eternalcode.minions.event.MinionEventCause;
import com.eternalcode.minions.event.EventDispatcher;
import com.eternalcode.minions.event.MinionPreCreateEvent;
import com.eternalcode.minions.event.MinionPreRemoveEvent;
import com.eternalcode.minions.event.MinionRemovedEvent;
import com.eternalcode.minions.event.MinionUpdatedEvent;
import com.eternalcode.minions.event.MinionUpdateType;
import com.eternalcode.minions.minion.access.MinionAccessGuard;
import com.eternalcode.minions.database.MinionData;
import com.eternalcode.minions.database.MinionPersistenceService;
import com.eternalcode.minions.item.MinionItemFactory;
import com.eternalcode.minions.minion.behavior.MinionBehavior;
import com.eternalcode.minions.minion.behavior.MinionBehaviorRegistry;
import com.eternalcode.minions.minion.storage.MinionItemTransferService;
import com.eternalcode.minions.minion.storage.MinionStorage;
import com.eternalcode.minions.minion.status.MinionStatusTracker;
import com.eternalcode.minions.minion.upgrade.UpgradeKind;
import com.eternalcode.minions.render.MinionRenderService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class MinionLifecycleService {

    private final MinionRegistry minions;
    private final MinionActionEngine actions;
    private final MinionRenderService renders;
    private final MinionPersistenceService persistence;
    private final MinionItemFactory items;
    private final MinionBehaviorRegistry behaviors;
    private final MinionAccessGuard access;
    private final MinionItemTransferService transfers;
    private final MinionStatusTracker statuses;
    private final EventDispatcher events;

    public MinionLifecycleService(
        MinionRegistry minions,
        MinionActionEngine actions,
        MinionRenderService renders,
        MinionPersistenceService persistence,
        MinionItemFactory items,
        MinionBehaviorRegistry behaviors,
        MinionAccessGuard access,
        MinionItemTransferService transfers,
        MinionStatusTracker statuses,
        EventDispatcher events
    ) {
        this.minions = minions;
        this.actions = actions;
        this.renders = renders;
        this.persistence = persistence;
        this.items = items;
        this.behaviors = behaviors;
        this.access = access;
        this.transfers = transfers;
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
            this.actions.add(minion);
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
        this.actions.add(minion);
        this.renders.showToNearby(minion);
        this.persistence.create(minion);
        this.events.fire(new MinionCreatedEvent(this.snapshot(minion), cause, actorId));
        return true;
    }

    public boolean remove(MinionId minionId, MinionEventCause cause, UUID actorId) {
        Minion minion = this.minions.findMinion(minionId).orElse(null);
        if (minion == null) {
            return false;
        }

        MinionSnapshot snapshot = this.snapshot(minion);
        MinionPreRemoveEvent event = this.events.fire(new MinionPreRemoveEvent(snapshot, cause, actorId));
        if (event.isCancelled() || this.minions.remove(minionId).isEmpty()) {
            return false;
        }

        this.actions.remove(minion);
        this.renders.remove(minion);
        this.statuses.remove(minionId);
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

    public boolean pickup(Player player, MinionId minionId) {
        Minion accessibleMinion = this.access.findAccessible(
                player,
                minionId,
                MinionAccessAction.PICK_UP
        ).orElse(null);

        if (accessibleMinion == null) {
            return false;
        }

        MinionSnapshot snapshot = this.snapshot(accessibleMinion);
        MinionPreRemoveEvent event = this.events.fire(new MinionPreRemoveEvent(
            snapshot,
            MinionEventCause.PICKUP,
            player.getUniqueId()
        ));
        if (event.isCancelled()) {
            return false;
        }

        Optional<Minion> removed = this.minions.remove(accessibleMinion.id());
        if (removed.isEmpty()) {
            return false;
        }
        Minion current = removed.get();
        this.actions.remove(current);
        this.renders.remove(current);
        this.statuses.remove(current.id());

        MinionStorage storage = current.storage();
        for (int slot = 0; slot < storage.capacity(); slot++) {
            ItemStack item = storage.item(slot);
            if (item != null) {
                this.transfers.giveOrDrop(player, item);
            }
        }

        // Storage contents are handed out as loose items above, so the minion item itself
        // must carry an emptied storage — otherwise placing it again would duplicate them.
        Minion emptied = current.withStorage(new MinionStorage(storage.capacity()));
        this.transfers.giveOrDrop(player, this.items.create(emptied));

        this.persistence.delete(current.id());
        this.events.fire(new MinionRemovedEvent(
            snapshot,
            MinionEventCause.PICKUP,
            player.getUniqueId()
        ));
        return true;
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
