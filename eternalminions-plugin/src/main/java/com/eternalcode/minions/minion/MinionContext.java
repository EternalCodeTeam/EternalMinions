package com.eternalcode.minions.minion;

import com.eternalcode.minions.minion.storage.MinionStorage;
import com.eternalcode.minions.minion.storage.MinionStorageUpdate;
import com.eternalcode.minions.minion.tool.ToolInventoryLocator;
import com.eternalcode.minions.minion.tool.ToolRequirement;
import com.eternalcode.minions.minion.tool.ToolValidationService;
import java.util.Collection;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.inventory.ItemStack;

public record MinionContext(Minion minion, World world, ScheduledMinion scheduledMinion) {

    public MinionContext {
        if (minion == null || world == null || scheduledMinion == null) {
            throw new IllegalArgumentException("Minion execution context requires minion, world and schedule");
        }
    }

    public boolean hasStorageRoom() {
        return this.minion.storage().hasFreeSlot() || this.linkedChest() != null;
    }

    public Container linkedChest() {
        MinionPosition chestPosition = this.minion.chestPosition();
        if (chestPosition == null || !chestPosition.worldKey().equals(this.minion.position().worldKey())) {
            return null;
        }
        if (!this.world.isChunkLoaded(chestPosition.blockX() >> 4, chestPosition.blockZ() >> 4)) {
            return null;
        }

        Block block = this.world.getBlockAt(
            chestPosition.blockX(),
            chestPosition.blockY(),
            chestPosition.blockZ()
        );
        return block.getState(false) instanceof Container container ? container : null;
    }

    public Minion deposit(Minion updatedMinion, Location overflowLocation, Collection<ItemStack> drops) {
        Container chest = this.linkedChest();
        MinionStorage storage = updatedMinion.storage();
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
                this.world.dropItemNaturally(overflowLocation, update.remaining());
            }
        }
        return updatedMinion.withStorage(storage);
    }

    public Minion retireWornTool(
        Minion currentMinion,
        ToolRequirement requirement,
        ToolValidationService validation,
        ToolInventoryLocator locator
    ) {
        ItemStack tool = currentMinion.equipment().tool();
        if (tool == null || validation.remainingDurability(tool) > requirement.minDurabilityToKeep()) {
            return currentMinion;
        }

        Container chest = this.linkedChest();
        MinionStorage storage = currentMinion.storage();
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
            this.world.dropItemNaturally(this.location(), overflow);
        }

        ItemStack replacement = null;
        int storageSlot = locator.findInStorage(storage.snapshot(), requirement);
        if (storageSlot >= 0) {
            replacement = storage.item(storageSlot);
            storage = storage.withItem(storageSlot, null);
        }
        else if (chest != null) {
            int chestSlot = locator.findInInventory(chest.getInventory(), requirement);
            if (chestSlot >= 0) {
                replacement = chest.getInventory().getItem(chestSlot);
                chest.getInventory().setItem(chestSlot, null);
            }
        }

        return currentMinion
            .withEquipment(currentMinion.equipment().withTool(replacement))
            .withStorage(storage);
    }

    public Location location() {
        return new Location(
            this.world,
            this.minion.position().blockX() + 0.5D,
            this.minion.position().blockY(),
            this.minion.position().blockZ() + 0.5D
        );
    }
}
