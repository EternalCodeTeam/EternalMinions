package com.eternalcode.minions.minion.behavior.impl.lumberjack;

import com.eternalcode.minions.minion.status.MinionStatus;

public final class LumberjackStatuses {

    public static final MinionStatus CUTTING = MinionStatus.of("CUTTING");
    public static final MinionStatus WAITING_FOR_TREE = MinionStatus.of("WAITING_FOR_TREE");
    public static final MinionStatus NO_SAPLING = MinionStatus.of("NO_SAPLING");
    public static final MinionStatus INVALID_STATION = MinionStatus.of("INVALID_STATION");
    public static final MinionStatus TREE_TOO_LARGE = MinionStatus.of("TREE_TOO_LARGE");
    public static final MinionStatus CANOPY_TOO_LARGE = MinionStatus.of("CANOPY_TOO_LARGE");
    public static final MinionStatus NO_AXE = MinionStatus.of("NO_AXE");

    private LumberjackStatuses() {
    }
}
