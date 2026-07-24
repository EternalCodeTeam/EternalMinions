package com.eternalcode.minions.minion;

import com.eternalcode.minions.database.MinionPersistenceService;
import com.eternalcode.minions.item.MinionItemFactory;
import com.eternalcode.minions.render.MinionRenderService;
import java.util.Optional;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class MinionLifecycleService {

    private final MinionRegistry minions;
    private final MinionActionEngine actions;
    private final MinionRenderService renders;
    private final MinionPersistenceService persistence;
    private final MinionItemFactory items;

    public MinionLifecycleService(
        MinionRegistry minions,
        MinionActionEngine actions,
        MinionRenderService renders,
        MinionPersistenceService persistence,
        MinionItemFactory items
    ) {
        this.minions = minions;
        this.actions = actions;
        this.renders = renders;
        this.persistence = persistence;
        this.items = items;
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

    public void updateUpgrade(Minion minion, MinionUpgradeKind upgrade) {
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

    public void pickup(Player player, Minion minion) {
        Optional<Minion> removed = this.minions.remove(minion.id());
        if (removed.isEmpty()) {
            return;
        }

        Minion current = removed.get();
        this.actions.remove(current);
        this.renders.remove(current);

        MinionStorage storage = current.storage();
        for (int slot = 0; slot < storage.capacity(); slot++) {
            ItemStack item = storage.item(slot);
            if (item != null) {
                this.giveOrDrop(player, item);
            }
        }

        // Storage contents are handed out as loose items above, so the minion item itself
        // must carry an emptied storage — otherwise placing it again would duplicate them.
        Minion emptied = current.withStorage(new MinionStorage(storage.capacity()));
        this.giveOrDrop(player, this.items.create(emptied));

        this.persistence.delete(current.id());
    }

    private void giveOrDrop(Player player, ItemStack item) {
        for (ItemStack remaining : player.getInventory().addItem(item).values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), remaining);
        }
    }
}
