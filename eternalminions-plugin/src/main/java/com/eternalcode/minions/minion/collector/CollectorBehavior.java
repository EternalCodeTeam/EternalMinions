package com.eternalcode.minions.minion.collector;

import com.eternalcode.minions.database.MinionPersistenceService;
import com.eternalcode.minions.minion.AbstractMinionBehavior;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionRegistry;
import com.eternalcode.minions.minion.MinionStorage;
import com.eternalcode.minions.minion.MinionStorageUpdate;
import com.eternalcode.minions.minion.MinionType;
import com.eternalcode.minions.minion.ScheduledMinion;
import com.eternalcode.minions.render.MinionRenderer;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Container;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

// Picks up dropped items around the minion into its storage (or the linked chest).
public final class CollectorBehavior extends AbstractMinionBehavior {

    private static final int MAX_ITEMS_PER_ACTION = 8;

    public CollectorBehavior(MinionRegistry registry, MinionPersistenceService persistence, MinionRenderer renderer) {
        super(registry, persistence, renderer);
    }

    @Override
    public boolean execute(Minion minion, MinionType type, ScheduledMinion scheduledMinion, World world) {
        if (!this.hasStorageRoom(minion, world)) {
            return false;
        }

        double radius = type.work().collectorRadius();
        Location center = this.minionLocation(minion, world);
        Container chest = this.resolveLinkedChest(minion, world);
        MinionStorage storage = minion.storage();

        boolean collectedAny = false;
        int processed = 0;
        for (Entity entity : world.getNearbyEntities(center, radius, radius, radius, Item.class::isInstance)) {
            if (processed >= MAX_ITEMS_PER_ACTION) {
                break;
            }
            processed++;

            Item itemEntity = (Item) entity;
            ItemStack stack = itemEntity.getItemStack();
            ItemStack remaining = stack;
            if (chest != null) {
                Map<Integer, ItemStack> chestLeftover = chest.getInventory().addItem(remaining);
                remaining = chestLeftover.isEmpty() ? null : chestLeftover.get(0);
            }
            if (remaining != null) {
                MinionStorageUpdate update = storage.add(remaining);
                storage = update.storage();
                remaining = update.remaining();
            }

            if (remaining == null) {
                itemEntity.remove();
                collectedAny = true;
            }
            else {
                itemEntity.setItemStack(remaining);
            }
        }

        if (!collectedAny) {
            return false;
        }
        this.commit(minion, type, storage, Float.NaN);
        return true;
    }
}
