package com.eternalcode.minions.minion.behavior.impl.collector;

import com.eternalcode.minions.minion.status.MinionStatus;

public final class CollectorStatuses {

    public static final MinionStatus COLLECTING = MinionStatus.of("COLLECTING");
    public static final MinionStatus NO_ITEMS_ON_GROUND = MinionStatus.of("NO_ITEMS_ON_GROUND");
    public static final MinionStatus NO_SHOVEL = MinionStatus.of("NO_SHOVEL");

    private CollectorStatuses() {
    }
}
