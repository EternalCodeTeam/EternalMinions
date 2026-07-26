package com.eternalcode.minions.minion.impl.lumberjack;

import com.eternalcode.minions.minion.status.MinionStatus;

public final class LumberjackStatuses {

    public static final MinionStatus CUTTING = new MinionStatus("CUTTING");
    public static final MinionStatus WAITING_FOR_TREE = new MinionStatus("WAITING_FOR_TREE");
    public static final MinionStatus NO_SAPLING = new MinionStatus("NO_SAPLING");
    public static final MinionStatus INVALID_STATION = new MinionStatus("INVALID_STATION");
    public static final MinionStatus NO_AXE = new MinionStatus("NO_AXE");

    private LumberjackStatuses() {
    }
}
