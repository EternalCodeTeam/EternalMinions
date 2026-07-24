package com.eternalcode.minions.minion;

import com.eternalcode.minions.database.MinionPersistenceService;
import com.eternalcode.minions.database.MinionRepository;
import com.eternalcode.minions.item.MinionItemFactory;
import com.eternalcode.minions.render.MinionRenderService;
import com.eternalcode.minions.scheduler.MinionActionEngine;
import java.util.Optional;
import java.util.logging.Level;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class MinionLifecycleService {

    private final MinionRegistry minions;
    private final MinionActionEngine actions;
    private final MinionRenderService renders;
    private final MinionPersistenceService persistence;
    private final MinionRepository repository;
    private final MinionItemFactory items;

    public MinionLifecycleService(
        MinionRegistry minions,
        MinionActionEngine actions,
        MinionRenderService renders,
        MinionPersistenceService persistence,
        MinionRepository repository,
        MinionItemFactory items
    ) {
        this.minions = minions;
        this.actions = actions;
        this.renders = renders;
        this.persistence = persistence;
        this.repository = repository;
        this.items = items;
    }

    public void add(Minion minion) {
        this.minions.register(minion);
        this.actions.add(minion);
        this.renders.showToNearby(minion);
        this.persistence.saveNow(minion);
    }

    public void update(Minion minion) {
        this.minions.replace(minion);
        this.persistence.changed(minion);
        this.renders.refreshEquipment(minion);
        this.renders.refreshRotation(minion);
    }

    // Purchases and chest links must survive a crash, so they skip the dirty-flush delay.
    public void updateNow(Minion minion) {
        this.minions.replace(minion);
        this.renders.refreshEquipment(minion);
        this.renders.refreshRotation(minion);
        this.persistence.saveNow(minion);
    }

    public void pickup(Player player, Minion minion) {
        Optional<Minion> removed = this.minions.remove(minion.id());
        if (removed.isEmpty()) {
            return;
        }

        Minion current = removed.get();
        this.actions.remove(current);
        this.renders.remove(current);
        this.persistence.forget(current.id());

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

        this.repository.delete(current.id().value()).exceptionally(error -> {
            player.getServer().getLogger().log(Level.SEVERE, "Unable to delete minion " + current.id().value(), error);
            return null;
        });
    }

    private void giveOrDrop(Player player, ItemStack item) {
        for (ItemStack remaining : player.getInventory().addItem(item).values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), remaining);
        }
    }
}
