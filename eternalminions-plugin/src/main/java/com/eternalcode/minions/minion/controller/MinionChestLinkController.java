package com.eternalcode.minions.minion.controller;

import com.eternalcode.minions.config.MessagesConfig;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionLifecycleService;
import com.eternalcode.minions.minion.MinionPosition;
import com.eternalcode.minions.minion.MinionRegistry;
import com.eternalcode.minions.notice.NoticeService;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public final class MinionChestLinkController implements Listener {

    private final MinionRegistry minions;
    private final MinionLifecycleService lifecycle;
    private final MessagesConfig messages;
    private final NoticeService notices;

    public MinionChestLinkController(
            MinionRegistry minions,
            MinionLifecycleService lifecycle,
            MessagesConfig messages,
            NoticeService notices
    ) {
        this.minions = minions;
        this.lifecycle = lifecycle;
        this.messages = messages;
        this.notices = notices;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        this.unlink(event.getBlock(), List.of());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        this.unlink(event.getBlock(), List.of());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        this.unlinkAll(event.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        this.unlinkAll(event.blockList());
    }

    private void unlinkAll(List<Block> destroyed) {
        for (Block block : List.copyOf(destroyed)) {
            this.unlink(block, destroyed);
        }
    }

    private void unlink(Block block, List<Block> destroyed) {
        if (!(block.getState(false) instanceof Container)) {
            return;
        }

        List<Minion> linked = this.minions.findLinkedToChest(
                block.getWorld().getKey().asString(),
                block.getX(),
                block.getY(),
                block.getZ()
        );

        if (linked.isEmpty()) {
            return;
        }

        MinionPosition remainingHalf = this.remainingHalf(block, destroyed);

        for (Minion minion : linked) {
            if (remainingHalf != null) {
                this.lifecycle.updateChestLink(minion.withChestPosition(remainingHalf));
                continue;
            }
            this.lifecycle.updateChestLink(minion.withChestPosition(null));
            this.notifyOwner(minion);
        }
    }

    private MinionPosition remainingHalf(Block block, List<Block> destroyed) {
        if (!(block.getState(false) instanceof Chest chest)
                || !(chest.getInventory() instanceof DoubleChestInventory doubleChest)) {
            return null;
        }

        Block left = holderBlock(doubleChest.getLeftSide().getHolder());
        Block right = holderBlock(doubleChest.getRightSide().getHolder());
        Block other = block.equals(left) ? right : block.equals(right) ? left : null;

        if (other == null || destroyed.contains(other)) {
            return null;
        }

        return new MinionPosition(
                other.getWorld().getKey().asString(),
                other.getX(),
                other.getY(),
                other.getZ()
        );
    }

    private static Block holderBlock(InventoryHolder holder) {
        return holder instanceof Chest chest ? chest.getBlock() : null;
    }

    private void notifyOwner(Minion minion) {
        Player owner = Bukkit.getPlayer(minion.ownerId());

        if (owner == null) {
            return;
        }

        this.notices.create()
                .viewer(owner)
                .notice(this.messages.chestLinkDestroyed)
                .send();
    }
}
