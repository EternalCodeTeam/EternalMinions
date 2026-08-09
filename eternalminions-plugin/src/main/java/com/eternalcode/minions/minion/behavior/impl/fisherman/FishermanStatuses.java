package com.eternalcode.minions.minion.behavior.impl.fisherman;

import com.eternalcode.minions.minion.status.MinionStatus;

public final class FishermanStatuses {

    public static final MinionStatus CATCHING = MinionStatus.of("CATCHING");
    public static final MinionStatus NOTHING_CAUGHT = MinionStatus.of("NOTHING_CAUGHT");
    public static final MinionStatus NO_WATER_NEARBY = MinionStatus.of("NO_WATER_NEARBY");
    public static final MinionStatus WATER_TOO_SMALL = MinionStatus.of("WATER_TOO_SMALL");
    public static final MinionStatus WATER_TOO_SHALLOW = MinionStatus.of("WATER_TOO_SHALLOW");
    public static final MinionStatus WATER_SURFACE_BLOCKED = MinionStatus.of("WATER_SURFACE_BLOCKED");
    public static final MinionStatus NO_ROD = MinionStatus.of("NO_ROD");

    private FishermanStatuses() {
    }
}
