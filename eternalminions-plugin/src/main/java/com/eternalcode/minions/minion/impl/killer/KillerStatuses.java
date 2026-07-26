package com.eternalcode.minions.minion.impl.killer;

import com.eternalcode.minions.minion.status.MinionStatus;

public final class KillerStatuses {

    public static final MinionStatus ATTACKING = new MinionStatus("ATTACKING");
    public static final MinionStatus NO_ENEMIES = new MinionStatus("NO_ENEMIES");
    public static final MinionStatus NO_WEAPON = new MinionStatus("NO_WEAPON");

    private KillerStatuses() {
    }
}
