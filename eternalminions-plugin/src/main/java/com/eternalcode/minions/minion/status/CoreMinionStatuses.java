package com.eternalcode.minions.minion.status;

public final class CoreMinionStatuses {

    public static final MinionStatus IDLE = MinionStatus.of("IDLE");
    public static final MinionStatus NO_TOOL = MinionStatus.of("NO_TOOL");
    public static final MinionStatus STORAGE_FULL = MinionStatus.of("STORAGE_FULL");
    public static final MinionStatus OFFLINE = MinionStatus.of("OFFLINE");
    public static final MinionStatus AWAY = MinionStatus.of("AWAY");

    private CoreMinionStatuses() {
    }
}
