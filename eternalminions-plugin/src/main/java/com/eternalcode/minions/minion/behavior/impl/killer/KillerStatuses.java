package com.eternalcode.minions.minion.behavior.impl.killer;

import com.eternalcode.minions.minion.status.MinionStatus;

public final class KillerStatuses {

    public static final MinionStatus ATTACKING =
            new MinionStatus("ATTACKING");

    public static final MinionStatus NO_ENEMIES =
            new MinionStatus("NO_ENEMIES");

    public static final MinionStatus PROTECTED_MOBS_NEARBY =
            new MinionStatus("PROTECTED_MOBS_NEARBY");

    public static final MinionStatus NO_WEAPON =
            new MinionStatus("NO_WEAPON");

    private KillerStatuses() {
    }
}