package com.eternalcode.minions.minion.impl.collector;

import com.eternalcode.minions.minion.status.MinionStatus;

public final class CollectorStatuses {

    public static final MinionStatus COLLECTING = new MinionStatus("COLLECTING");
    public static final MinionStatus NO_ITEMS_ON_GROUND = new MinionStatus("NO_ITEMS_ON_GROUND");
    public static final MinionStatus NO_SHOVEL = new MinionStatus("NO_SHOVEL");

    private CollectorStatuses() {
    }
}
