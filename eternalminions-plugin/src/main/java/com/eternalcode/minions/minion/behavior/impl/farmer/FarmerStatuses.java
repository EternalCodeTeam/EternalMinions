package com.eternalcode.minions.minion.behavior.impl.farmer;

import com.eternalcode.minions.minion.status.MinionStatus;

public final class FarmerStatuses {

    public static final MinionStatus HARVESTING = MinionStatus.of("HARVESTING");
    public static final MinionStatus NO_MATURE_CROPS = MinionStatus.of("NO_MATURE_CROPS");
    public static final MinionStatus NO_SEEDS = MinionStatus.of("NO_SEEDS");
    public static final MinionStatus NO_HOE = MinionStatus.of("NO_HOE");
    public static final MinionStatus NO_CONFIGURED_CROPS = MinionStatus.of("NO_CONFIGURED_CROPS");

    private FarmerStatuses() {
    }
}
