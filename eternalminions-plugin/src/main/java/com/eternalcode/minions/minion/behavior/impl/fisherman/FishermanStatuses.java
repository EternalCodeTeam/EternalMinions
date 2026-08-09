package com.eternalcode.minions.minion.behavior.impl.fisherman;

import com.eternalcode.minions.minion.status.MinionStatus;

public final class FishermanStatuses {

    public static final MinionStatus FISHING =
            new MinionStatus("FISHING");

    public static final MinionStatus CATCHING =
            new MinionStatus("CATCHING");
    public static final MinionStatus NOTHING_CAUGHT =
            new MinionStatus("NOTHING_CAUGHT");

    public static final MinionStatus NO_WATER_NEARBY =
            new MinionStatus("NO_WATER_NEARBY");

    public static final MinionStatus WATER_TOO_SMALL =
            new MinionStatus("WATER_TOO_SMALL");

    public static final MinionStatus WATER_TOO_SHALLOW =
            new MinionStatus("WATER_TOO_SHALLOW");

    public static final MinionStatus WATER_SURFACE_BLOCKED =
            new MinionStatus("WATER_SURFACE_BLOCKED");

    public static final MinionStatus NO_ROD =
            new MinionStatus("NO_ROD");

    private FishermanStatuses() {
    }
}
