package com.eternalcode.minions.minion;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

public final class MinionPistonProtectionListener implements Listener {

    private final MinionRegistry minions;

    public MinionPistonProtectionListener(MinionRegistry minions) {
        this.minions = minions;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        BlockFace movement = event.getDirection();

        if (this.intersectsMinion(
                event.getBlock().getRelative(movement)
        )) {
            event.setCancelled(true);
            return;
        }

        for (Block movedBlock : event.getBlocks()) {
            if (this.intersectsMinion(movedBlock)
                    || this.intersectsMinion(
                            movedBlock.getRelative(movement)
                    )) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        BlockFace movement = event.getDirection().getOppositeFace();

        for (Block movedBlock : event.getBlocks()) {
            if (this.intersectsMinion(movedBlock)
                    || this.intersectsMinion(
                            movedBlock.getRelative(movement)
                    )) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private boolean intersectsMinion(Block block) {
        String worldKey = block.getWorld().getKey().asString();

        return this.minions.hasMinionAt(
                worldKey,
                block.getX(),
                block.getY() + 1,
                block.getZ()
        ) || this.minions.hasMinionAt(
                worldKey,
                block.getX(),
                block.getY(),
                block.getZ()
        ) || this.minions.hasMinionAt(
                worldKey,
                block.getX(),
                block.getY() - 1,
                block.getZ()
        );
    }
}
