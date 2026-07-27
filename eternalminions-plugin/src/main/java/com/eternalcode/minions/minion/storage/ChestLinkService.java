package com.eternalcode.minions.minion.storage;

import com.eternalcode.minions.access.MinionAccessAction;
import com.eternalcode.minions.minion.access.MinionAccessGuard;
import com.eternalcode.minions.config.MessagesConfig;
import com.eternalcode.minions.config.MinionsConfig;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionId;
import com.eternalcode.minions.minion.MinionPosition;
import com.eternalcode.minions.notice.NoticeService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class ChestLinkService implements Listener {

    private final MinionAccessGuard access;
    private final MinionsConfig config;
    private final Consumer<Minion> update;
    private final MessagesConfig messages;
    private final NoticeService notices;
    private final Cache<UUID, MinionId> pendingLinks;

    public ChestLinkService(
            MinionAccessGuard access,
            MinionsConfig config,
            Consumer<Minion> update,
            MessagesConfig messages,
            NoticeService notices
    ) {
        this.access = access;
        this.config = config;
        this.update = update;
        this.messages = messages;
        this.notices = notices;
        this.pendingLinks = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(config.chestLinkTimeoutSeconds))
                .build();
    }

    public void toggle(Player player, Minion minion) {
        Minion current = this.access.findAccessible(
                player,
                minion.id(),
                MinionAccessAction.MANAGE
        ).orElse(null);

        if (current == null) {
            return;
        }

        minion = current;
        UUID playerId = player.getUniqueId();

        if (minion.chestPosition() != null) {
            this.pendingLinks.invalidate(playerId);
            this.update.accept(minion.withChestPosition(null));

            this.notices.create()
                    .viewer(player)
                    .notice(this.messages.chestUnlinked)
                    .send();

            return;
        }

        this.pendingLinks.put(playerId, minion.id());

        this.notices.create()
                .viewer(player)
                .notice(this.messages.chestLinkStart)
                .send();

        player.closeInventory();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChestClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        MinionId minionId = this.pendingLinks.getIfPresent(player.getUniqueId());

        if (minionId == null) {
            return;
        }

        Block block = event.getClickedBlock();

        if (block == null || !(block.getState(false) instanceof Container)) {
            return;
        }

        this.pendingLinks.invalidate(player.getUniqueId());
        event.setCancelled(true);

        Minion minion = this.access.findAccessible(
                player,
                minionId,
                MinionAccessAction.MANAGE
        ).orElse(null);

        if (minion == null) {
            return;
        }

        if (!this.withinLinkDistance(minion, block)) {
            this.notices.create()
                    .viewer(player)
                    .notice(this.messages.chestLinkTooFar)
                    .send();

            return;
        }

        MinionPosition chestPosition = new MinionPosition(
                block.getWorld().getKey().asString(),
                block.getX(),
                block.getY(),
                block.getZ()
        );

        this.update.accept(minion.withChestPosition(chestPosition));

        this.notices.create()
                .viewer(player)
                .notice(this.messages.chestLinked)
                .send();
    }

    private boolean withinLinkDistance(Minion minion, Block block) {
        MinionPosition position = minion.position();

        if (!position.worldKey().equals(block.getWorld().getKey().asString())) {
            return false;
        }

        long distanceX = (long) block.getX() - position.blockX();
        long distanceY = (long) block.getY() - position.blockY();
        long distanceZ = (long) block.getZ() - position.blockZ();
        long maxDistance = this.config.chestLinkDistanceBlocks;

        return distanceX * distanceX
                + distanceY * distanceY
                + distanceZ * distanceZ
                <= maxDistance * maxDistance;
    }
}
