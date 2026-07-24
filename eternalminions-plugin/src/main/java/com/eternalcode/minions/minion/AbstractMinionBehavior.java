package com.eternalcode.minions.minion;

import com.eternalcode.minions.database.MinionPersistenceService;
import com.eternalcode.minions.render.MinionRenderer;
import java.util.Collection;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.inventory.ItemStack;

// Shared plumbing for behaviors that deposit items and advance progress (mining, generators, collecting).
public abstract class AbstractMinionBehavior implements MinionBehavior {

    protected final MinionRegistry registry;
    protected final MinionPersistenceService persistence;
    protected final MinionRenderer renderer;

    protected AbstractMinionBehavior(MinionRegistry registry, MinionPersistenceService persistence, MinionRenderer renderer) {
        this.registry = registry;
        this.persistence = persistence;
        this.renderer = renderer;
    }

    protected final Container resolveLinkedChest(Minion minion, World world) {
        MinionPosition chestPosition = minion.chestPosition();
        if (chestPosition == null || !chestPosition.worldKey().equals(minion.position().worldKey())) {
            return null;
        }
        if (!world.isChunkLoaded(chestPosition.blockX() >> 4, chestPosition.blockZ() >> 4)) {
            return null;
        }

        Block block = world.getBlockAt(chestPosition.blockX(), chestPosition.blockY(), chestPosition.blockZ());
        return block.getState(false) instanceof Container container ? container : null;
    }

    // A minion with no free slot and no reachable chest cannot store anything, so it should idle rather than
    // spew items on the floor forever.
    protected final boolean hasStorageRoom(Minion minion, World world) {
        return minion.storage().hasFreeSlot() || this.resolveLinkedChest(minion, world) != null;
    }

    // Delivers drops to the linked chest first, then the internal storage, then the ground for the remainder.
    protected final void deposit(
        Minion minion,
        MinionType type,
        World world,
        Location overflowLocation,
        Collection<ItemStack> drops,
        float yaw
    ) {
        Container chest = this.resolveLinkedChest(minion, world);
        MinionStorage storage = minion.storage();
        for (ItemStack drop : drops) {
            ItemStack remaining = drop;
            if (chest != null) {
                Map<Integer, ItemStack> chestLeftover = chest.getInventory().addItem(remaining);
                remaining = chestLeftover.isEmpty() ? null : chestLeftover.get(0);
            }
            if (remaining == null) {
                continue;
            }

            MinionStorageUpdate update = storage.add(remaining);
            storage = update.storage();
            if (update.remaining() != null) {
                world.dropItemNaturally(overflowLocation, update.remaining());
            }
        }
        this.commit(minion, type, storage, yaw);
    }

    protected final void commit(Minion minion, MinionType type, MinionStorage storage, float yaw) {
        MinionProgress progress = minion.progress().advanced(type);
        Minion updated = minion.withStorage(storage).withProgress(progress);
        this.registry.replace(updated);
        this.persistence.saveState(updated);
        this.persistence.saveStorage(minion, updated);
        this.renderer.animate(minion.id(), yaw);
        if (progress.level() != minion.progress().level()) {
            this.renderer.refreshHologram(updated);
        }
    }

    protected final Location minionLocation(Minion minion, World world) {
        return new Location(world, minion.position().blockX() + 0.5D, minion.position().blockY(),
            minion.position().blockZ() + 0.5D);
    }
}
