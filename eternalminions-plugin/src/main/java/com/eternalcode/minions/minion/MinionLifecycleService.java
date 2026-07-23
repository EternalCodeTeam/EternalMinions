package com.eternalcode.minions.minion;

import com.eternalcode.minions.database.MinionPersistenceService;
import com.eternalcode.minions.database.MinionRepository;
import com.eternalcode.minions.item.MinionItemFactory;
import com.eternalcode.minions.render.MinionRenderService;
import com.eternalcode.minions.scheduler.MinionActionEngine;
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
    }

    public void pickup(Player player, Minion minion) {
        if (this.minions.remove(minion.id()).isEmpty()) {
            return;
        }

        this.actions.remove(minion);
        this.renders.remove(minion);
        this.persistence.forget(minion.id());
        for (ItemStack item : player.getInventory()
            .addItem(this.items.create())
            .values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        }
        this.repository.delete(minion.id().value()).exceptionally(error -> {
            player.getServer().getLogger().log(Level.SEVERE, "Unable to delete minion " + minion.id().value(), error);
            return null;
        });
    }
}
