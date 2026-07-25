package com.eternalcode.minions.minion;

import com.eternalcode.minions.database.MinionPersistenceService;
import com.eternalcode.minions.minion.status.MinionStatus;
import com.eternalcode.minions.minion.status.MinionStatusTracker;
import com.eternalcode.minions.minion.tool.ToolInventoryLocator;
import com.eternalcode.minions.minion.tool.ToolRequirement;
import com.eternalcode.minions.minion.tool.ToolValidationService;
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
    private final MinionStatusTracker statuses;

    protected AbstractMinionBehavior(
        MinionRegistry registry,
        MinionPersistenceService persistence,
        MinionRenderer renderer,
        MinionStatusTracker statuses
    ) {
        this.registry = registry;
        this.persistence = persistence;
        this.renderer = renderer;
        this.statuses = statuses;
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

    protected final void refreshStatusIfChanged(Minion minion, MinionStatus status) {
        if (this.statuses.setStatus(minion.id(), status)) {
            this.renderer.refreshHologram(minion);
        }
    }

    protected final MinionStatus currentStatus(Minion minion) {
        return this.statuses.status(minion.id());
    }

    // Once the equipped tool's remaining durability drops to or below the profession's configured
    // retirement threshold, moves it to the linked chest (falling back to storage, then the
    // ground) instead of letting it break, and pulls in a replacement from storage or the chest
    // if one is available. Returns the minion unchanged when there is nothing to retire.
    protected final Minion retireWornToolIfNeeded(
        Minion minion,
        MinionType type,
        World world,
        ToolValidationService toolValidation,
        ToolInventoryLocator toolLocator
    ) {
        ItemStack tool = minion.equipment().tool();
        if (tool == null) {
            return minion;
        }

        ToolRequirement requirement = type.work().toolRequirement();
        if (toolValidation.remainingDurability(tool) > requirement.minDurabilityToKeep()) {
            return minion;
        }

        Container chest = this.resolveLinkedChest(minion, world);
        MinionStorage storage = minion.storage();
        ItemStack overflow = tool;
        if (chest != null) {
            Map<Integer, ItemStack> chestLeftover = chest.getInventory().addItem(overflow);
            overflow = chestLeftover.isEmpty() ? null : chestLeftover.get(0);
        }
        if (overflow != null) {
            MinionStorageUpdate update = storage.add(overflow);
            storage = update.storage();
            overflow = update.remaining();
        }
        if (overflow != null) {
            world.dropItemNaturally(this.minionLocation(minion, world), overflow);
        }

        ItemStack replacement = null;
        int storageSlot = toolLocator.findInStorage(storage.snapshot(), requirement);
        if (storageSlot >= 0) {
            replacement = storage.item(storageSlot);
            storage = storage.withItem(storageSlot, null);
        }
        else if (chest != null) {
            int chestSlot = toolLocator.findInInventory(chest.getInventory(), requirement);
            if (chestSlot >= 0) {
                replacement = chest.getInventory().getItem(chestSlot);
                chest.getInventory().setItem(chestSlot, null);
            }
        }

        Minion updated = minion.withEquipment(minion.equipment().withTool(replacement)).withStorage(storage);
        this.registry.replace(updated);
        this.persistence.saveEquipment(updated);
        this.persistence.saveStorage(minion, updated);
        this.renderer.refreshEquipment(updated.id(), replacement);
        return updated;
    }
}
