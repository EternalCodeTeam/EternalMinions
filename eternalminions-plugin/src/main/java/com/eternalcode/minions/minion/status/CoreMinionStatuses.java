package com.eternalcode.minions.minion.status;

public final class CoreMinionStatuses {

    public static final MinionStatus IDLE = new MinionStatus("IDLE");
    public static final MinionStatus WORKING = new MinionStatus("WORKING");
    public static final MinionStatus NO_TOOL = new MinionStatus("NO_TOOL");
    public static final MinionStatus STORAGE_FULL = new MinionStatus("STORAGE_FULL");

    private CoreMinionStatuses() {
    }
}
