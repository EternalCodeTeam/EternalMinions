package com.eternalcode.minions.minion;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;

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

    public Location location() {
        return new Location(
            this.world,
            this.minion.position().blockX() + 0.5D,
            this.minion.position().blockY(),
            this.minion.position().blockZ() + 0.5D
        );
    }
}
