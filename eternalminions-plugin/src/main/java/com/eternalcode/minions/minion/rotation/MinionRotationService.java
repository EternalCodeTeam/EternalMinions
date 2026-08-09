package com.eternalcode.minions.minion.rotation;

import com.eternalcode.minions.access.MinionAccessAction;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionId;
import com.eternalcode.minions.minion.access.MinionAccessGuard;
import com.eternalcode.minions.minion.storage.MinionSettings;
import com.eternalcode.minions.notice.NoticeService;
import java.util.Optional;
import java.util.function.Consumer;
import org.bukkit.entity.Player;

public final class MinionRotationService {

    private final MinionAccessGuard access;
    private final Consumer<Minion> update;
    private final NoticeService noticeService;

    public MinionRotationService(
            MinionAccessGuard access,
            Consumer<Minion> update,
            NoticeService noticeService
    ) {
        this.access = access;
        this.update = update;
        this.noticeService = noticeService;
    }

    public Optional<Minion> rotate(Player player, MinionId minionId) {
        return this.access.findAccessible(player, minionId, MinionAccessAction.MANAGE)
                .map(minion -> this.rotate(player, minion));
    }

    private Minion rotate(Player player, Minion minion) {
        MinionSettings settings = minion.settings()
                .withDirection(minion.settings().direction().rotated());

        Minion rotated = minion.withSettings(settings);

        this.update.accept(rotated);
        this.noticeService.create()
                .viewer(player)
                .notice(messages -> messages.minionRotated)
                .send();

        return rotated;
    }
}
