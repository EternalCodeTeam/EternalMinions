package com.eternalcode.minions.minion.rotation;

import com.eternalcode.minions.access.MinionAccessAction;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionId;
import com.eternalcode.minions.minion.access.MinionAccessGuard;
import com.eternalcode.minions.minion.storage.MinionSettings;
import com.eternalcode.multification.notice.Notice;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.bukkit.entity.Player;

public final class MinionRotationService {

    private final MinionAccessGuard access;
    private final Consumer<Minion> update;
    private final BiConsumer<Player, Notice> notices;
    private final Notice rotatedNotice;

    public MinionRotationService(
            MinionAccessGuard access,
            Consumer<Minion> update,
            BiConsumer<Player, Notice> notices,
            Notice rotatedNotice
    ) {
        this.access = access;
        this.update = update;
        this.notices = notices;
        this.rotatedNotice = rotatedNotice;
    }

    public Optional<Minion> rotate(Player player, MinionId minionId) {
        return this.access
                .findAccessible(player, minionId, MinionAccessAction.MANAGE)
                .map(minion -> this.rotate(player, minion));
    }

    private Minion rotate(Player player, Minion minion) {
        MinionSettings settings = minion.settings()
                .withDirection(minion.settings().direction().rotated());

        Minion rotated = minion.withSettings(settings);

        this.update.accept(rotated);
        this.notices.accept(player, this.rotatedNotice);

        return rotated;
    }
}