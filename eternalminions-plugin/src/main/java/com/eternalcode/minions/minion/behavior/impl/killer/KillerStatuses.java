package com.eternalcode.minions.minion.behavior.impl.killer;

import com.eternalcode.minions.minion.status.MinionStatus;

public final class KillerStatuses {

    public static final MinionStatus ATTACKING = MinionStatus.of("ATTACKING");
    public static final MinionStatus NO_ENEMIES = MinionStatus.of("NO_ENEMIES");
    public static final MinionStatus PROTECTED_MOBS_NEARBY = MinionStatus.of("PROTECTED_MOBS_NEARBY");
    public static final MinionStatus NO_WEAPON = MinionStatus.of("NO_WEAPON");

    private KillerStatuses() {
    }
}
