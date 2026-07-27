package com.eternalcode.minions.minion;

import com.eternalcode.minions.access.MinionAccessAction;
import com.eternalcode.minions.minion.access.MinionAccessGuard;
import com.eternalcode.minions.database.MinionData;
import com.eternalcode.minions.database.MinionPersistenceService;
import com.eternalcode.minions.item.MinionItemFactory;
import com.eternalcode.minions.minion.storage.MinionItemTransferService;
import com.eternalcode.minions.minion.storage.MinionStorage;
import com.eternalcode.minions.minion.upgrade.UpgradeKind;
import com.eternalcode.minions.render.MinionRenderService;
import java.util.List;
import java.util.Optional;
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

    public MinionLifecycleService(
        MinionRegistry minions,
        MinionActionEngine actions,
        MinionRenderService renders,
        MinionPersistenceService persistence,
        MinionItemFactory items,
        MinionBehaviorRegistry behaviors,
        MinionAccessGuard access,
        MinionItemTransferService transfers
    ) {
        this.minions = minions;
        this.actions = actions;
        this.renders = renders;
        this.persistence = persistence;
        this.items = items;
        this.behaviors = behaviors;
        this.access = access;
        this.transfers = transfers;
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
        }
        return loaded.size();
    }

    public void add(Minion minion) {
        this.minions.register(minion);
        this.actions.add(minion);
        this.renders.showToNearby(minion);
        this.persistence.create(minion);
    }

    public void updateState(Minion minion) {
        this.minions.replace(minion);
        this.persistence.saveState(minion);
    }

    public void updateEquipment(Minion minion) {
        this.minions.replace(minion);
        this.persistence.saveEquipment(minion);
        this.renders.refreshEquipment(minion);
    }

    public void updateStorage(Minion minion) {
        Minion previous = this.minions.findMinion(minion.id()).orElse(null);
        if (previous == null) {
            return;
        }
        this.minions.replace(minion);
        this.persistence.saveStorage(previous, minion);
    }

    public void updateSettings(Minion minion) {
        this.minions.replace(minion);
        this.persistence.saveSettings(minion);
        this.renders.refreshRotation(minion);
    }

    public void updateUpgrade(Minion minion, UpgradeKind upgrade) {
        Minion previous = this.minions.findMinion(minion.id()).orElse(null);
        if (previous == null) {
            return;
        }
        this.minions.replace(minion);
        this.persistence.saveUpgrade(minion, upgrade);
        this.persistence.saveStorage(previous, minion);
    }

    public void updateChestLink(Minion minion) {
        this.minions.replace(minion);
        this.persistence.saveChestLink(minion);
    }

    public boolean pickup(Player player, Minion minion) {
        Minion accessibleMinion = this.access.findAccessible(
                player,
                minion.id(),
                MinionAccessAction.PICK_UP
        ).orElse(null);

        if (accessibleMinion == null) {
            return false;
        }

        Optional<Minion> removed = this.minions.remove(accessibleMinion.id());
        if (removed.isEmpty()) {
            return false;
        }

        Minion current = removed.get();
        this.actions.remove(current);
        this.renders.remove(current);

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
        return true;
    }

}
