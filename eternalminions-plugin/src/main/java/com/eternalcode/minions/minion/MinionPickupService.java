package com.eternalcode.minions.minion;

import com.eternalcode.minions.minion.limit.MinionLimitStatus;
import com.eternalcode.minions.minion.limit.PlayerMinionLimitService;
import com.eternalcode.minions.notice.NoticeService;
import org.bukkit.entity.Player;

public final class MinionPickupService {

    private final MinionLifecycleService lifecycle;
    private final PlayerMinionLimitService limits;
    private final NoticeService notices;

    public MinionPickupService(
            MinionLifecycleService lifecycle,
            PlayerMinionLimitService limits,
            NoticeService notices
    ) {
        this.lifecycle = lifecycle;
        this.limits = limits;
        this.notices = notices;
    }

    public void pickup(Player player, MinionId minionId) {
        if (!this.lifecycle.pickup(player, minionId)) {
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
}
