package com.eternalcode.minions;

import com.eternalcode.minions.config.MessagesConfig;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionLifecycleService;
import com.eternalcode.minions.minion.limit.MinionLimitStatus;
import com.eternalcode.minions.minion.limit.PlayerMinionLimitService;
import com.eternalcode.minions.notice.NoticeService;
import java.util.function.BiConsumer;
import org.bukkit.entity.Player;

public class MinionPickupHandler {

    BiConsumer<Player, Minion> createPickupHandler(
            MinionLifecycleService lifecycle,
            PlayerMinionLimitService limits,
            NoticeService notices,
            MessagesConfig messages
    ) {
        return (player, minion) -> {
            if (!lifecycle.pickup(player, minion)) {
                return;
            }
            MinionLimitStatus status = limits.statusFor(player);
            notices.create()
                    .viewer(player)
                    .notice(messages.minionPickedUp)
                    .placeholder("{MINION_LIMIT_CURRENT}", Integer.toString(status.current()))
                    .placeholder("{MINION_LIMIT_MAX}", status.maxDisplay())
                    .send();
            player.closeInventory();
        };
    }
}