package com.eternalcode.minions.minion.fisherman;

import com.eternalcode.minions.minion.status.MinionStatus;

public final class FisherStatuses {

    public static final MinionStatus FISHING = new MinionStatus("FISHING");
    public static final MinionStatus NO_WATER_NEARBY = new MinionStatus("NO_WATER_NEARBY");
    public static final MinionStatus WATER_TOO_SMALL = new MinionStatus("WATER_TOO_SMALL");
    public static final MinionStatus NO_ROD = new MinionStatus("NO_ROD");

    private FisherStatuses() {
    }
}
