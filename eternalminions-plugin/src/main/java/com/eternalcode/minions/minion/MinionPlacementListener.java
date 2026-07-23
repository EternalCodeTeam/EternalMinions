package com.eternalcode.minions.minion;

import com.eternalcode.minions.config.MessagesConfig;
import com.eternalcode.minions.item.MinionItemFactory;
import com.eternalcode.minions.notice.NoticeService;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class MinionPlacementListener implements Listener {

    private final MinionItemFactory items;
    private final MinionIdSequence ids;
    private final MinionLifecycleService lifecycle;
    private final MessagesConfig messages;
    private final NoticeService notices;

    public MinionPlacementListener(
        MinionItemFactory items,
        MinionIdSequence ids,
        MinionLifecycleService lifecycle,
        MessagesConfig messages,
        NoticeService notices
    ) {
        this.items = items;
        this.ids = ids;
        this.lifecycle = lifecycle;
        this.messages = messages;
        this.notices = notices;
    }

    @EventHandler
    public void onPlace(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (!this.items.isMinion(item)) {
            return;
        }
        event.setCancelled(true);

        Block target = event.getClickedBlock().getRelative(event.getBlockFace());
        Player player = event.getPlayer();
        if (!target.isEmpty() || !target.getRelative(0, 1, 0).isEmpty()) {
            this.notices.create().viewer(player).notice(this.messages.minionPlacementBlocked).send();
            return;
        }

        Minion minion = new Minion(
            this.ids.next(),
            player.getUniqueId(),
            "miner",
            new MinionPosition(target.getWorld().getKey().asString(), target.getX(), target.getY(), target.getZ()),
            true,
            new MinionProgress(1),
            MinionEquipment.empty(),
            new MinionStorage(9)
        );
        this.lifecycle.add(minion);

        item.subtract(1);
        this.notices.create().viewer(player).notice(this.messages.minionPlaced).send();
    }
}
