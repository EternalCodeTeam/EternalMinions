package com.eternalcode.minions.minion.access;

import com.eternalcode.minions.access.MinionAccessAction;
import com.eternalcode.minions.access.MinionAccessService;
import com.eternalcode.minions.config.MessagesConfig;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionId;
import com.eternalcode.minions.minion.MinionRegistry;
import com.eternalcode.minions.notice.NoticeService;
import java.util.Optional;
import org.bukkit.entity.Player;

public final class MinionAccessGuard {

    private final MinionRegistry minions;
    private final MinionAccessService access;
    private final MessagesConfig messages;
    private final NoticeService notices;

    public MinionAccessGuard(
            MinionRegistry minions,
            MinionAccessService access,
            MessagesConfig messages,
            NoticeService notices
    ) {
        this.minions = minions;
        this.access = access;
        this.messages = messages;
        this.notices = notices;
    }

    public Optional<Minion> findAccessible(
            Player player,
            MinionId minionId,
            MinionAccessAction action
    ) {
        Minion minion = this.minions.findMinion(minionId).orElse(null);

        if (minion == null) {
            this.send(player, this.messages.minionNotFound);
            return Optional.empty();
        }

        if (!this.access.canAccess(player, minion.details(), action)) {
            this.send(player, this.messages.minionOwnerRequired);
            return Optional.empty();
        }

        return Optional.of(minion);
    }

    private void send(
            Player player,
            com.eternalcode.multification.notice.Notice notice
    ) {
        this.notices.create()
                .viewer(player)
                .notice(notice)
                .send();
    }
}
