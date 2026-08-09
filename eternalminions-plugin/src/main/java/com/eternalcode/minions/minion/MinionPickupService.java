package com.eternalcode.minions.minion;

import com.eternalcode.minions.access.MinionAccessAction;
import com.eternalcode.minions.event.MinionEventCause;
import com.eternalcode.minions.item.MinionItemFactory;
import com.eternalcode.minions.minion.access.MinionAccessGuard;
import com.eternalcode.minions.minion.limit.MinionLimitStatus;
import com.eternalcode.minions.minion.limit.PlayerMinionLimitService;
import com.eternalcode.minions.minion.storage.MinionItemTransferService;
import com.eternalcode.minions.minion.storage.MinionStorage;
import com.eternalcode.minions.notice.NoticeService;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class MinionPickupService {

    private final MinionLifecycleService lifecycle;
    private final MinionAccessGuard access;
    private final MinionItemTransferService transfers;
    private final MinionItemFactory items;
    private final PlayerMinionLimitService limits;
    private final NoticeService notices;

    public MinionPickupService(
            MinionLifecycleService lifecycle,
            MinionAccessGuard access,
            MinionItemTransferService transfers,
            MinionItemFactory items,
            PlayerMinionLimitService limits,
            NoticeService notices
    ) {
        this.lifecycle = lifecycle;
        this.access = access;
        this.transfers = transfers;
        this.items = items;
        this.limits = limits;
        this.notices = notices;
    }

    public void pickup(Player player, MinionId minionId) {
        Minion minion = this.access.findAccessible(player, minionId, MinionAccessAction.PICK_UP).orElse(null);
        if (minion == null || !this.lifecycle.remove(
            minionId,
            MinionEventCause.PICKUP,
            player.getUniqueId(),
            removed -> this.giveContents(player, removed)
        )) {
            return;
        }

        MinionLimitStatus status = this.limits.statusFor(player);
        this.notices.create()
                .viewer(player)
                .notice(messagesConfig -> messagesConfig.minionPickedUp)
                .placeholder("{MINION_LIMIT_CURRENT}", Integer.toString(status.current()))
                .placeholder("{MINION_LIMIT_MAX}", status.maxDisplay())
                .send();
        player.closeInventory();
    }

    private void giveContents(Player player, Minion minion) {
        MinionStorage storage = minion.storage();
        for (int slot = 0; slot < storage.capacity(); slot++) {
            ItemStack item = storage.item(slot);
            if (item != null) {
                this.transfers.giveOrDrop(player, item);
            }
        }

        Minion emptied = minion.withStorage(new MinionStorage(storage.capacity()));
        this.transfers.giveOrDrop(player, this.items.create(emptied));
    }
}
