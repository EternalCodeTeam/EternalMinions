package com.eternalcode.minions.minion;

import com.eternalcode.minions.config.MessagesConfig;
import com.eternalcode.minions.config.MinionsConfig;
import com.eternalcode.minions.notice.NoticeService;
import com.eternalcode.multification.notice.Notice;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class ChestLinkService implements Listener {

    private final MinionRegistry minions;
    private final MinionsConfig config;
    private final Consumer<Minion> update;
    private final MessagesConfig messages;
    private final NoticeService notices;
    private final Map<UUID, PendingLink> pendingLinks = new HashMap<>();

    public ChestLinkService(
        MinionRegistry minions,
        MinionsConfig config,
        Consumer<Minion> update,
        MessagesConfig messages,
        NoticeService notices
    ) {
        this.minions = minions;
        this.config = config;
        this.update = update;
        this.messages = messages;
        this.notices = notices;
    }

    public void toggle(Player player, Minion minion) {
        if (minion.chestPosition() != null) {
            this.update.accept(minion.withChestPosition(null));
            this.send(player, this.messages.chestUnlinked);
            return;
        }

        long deadline = System.currentTimeMillis() + this.config.chestLinkTimeoutSeconds * 1_000L;
        this.pendingLinks.put(player.getUniqueId(), new PendingLink(minion.id(), deadline));
        this.send(player, this.messages.chestLinkStart);
        player.closeInventory();
    }

    @EventHandler
    public void onChestClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        PendingLink pending = this.pendingLinks.get(player.getUniqueId());
        if (pending == null) {
            return;
        }

        Block block = event.getClickedBlock();
        if (!(block.getState(false) instanceof Container)) {
            return;
        }

        this.pendingLinks.remove(player.getUniqueId());
        event.setCancelled(true);
        if (System.currentTimeMillis() > pending.deadline()) {
            this.send(player, this.messages.chestLinkExpired);
            return;
        }

        Minion minion = this.minions.findMinion(pending.minionId()).orElse(null);
        if (minion == null) {
            this.send(player, this.messages.minionNotFound);
            return;
        }
        if (!this.withinLinkDistance(minion, block)) {
            this.send(player, this.messages.chestLinkTooFar);
            return;
        }

        MinionPosition chestPosition =
            new MinionPosition(block.getWorld().getKey().asString(), block.getX(), block.getY(), block.getZ());
        this.update.accept(minion.withChestPosition(chestPosition));
        this.send(player, this.messages.chestLinked);
    }

    private boolean withinLinkDistance(Minion minion, Block block) {
        if (!minion.position().worldKey().equals(block.getWorld().getKey().asString())) {
            return false;
        }
        long distanceX = block.getX() - minion.position().blockX();
        long distanceY = block.getY() - minion.position().blockY();
        long distanceZ = block.getZ() - minion.position().blockZ();
        long maxDistance = this.config.chestLinkDistanceBlocks;
        return distanceX * distanceX + distanceY * distanceY + distanceZ * distanceZ <= maxDistance * maxDistance;
    }

    private void send(Player player, Notice notice) {
        this.notices.create().viewer(player).notice(notice).send();
    }

    private record PendingLink(MinionId minionId, long deadline) {
    }
}
