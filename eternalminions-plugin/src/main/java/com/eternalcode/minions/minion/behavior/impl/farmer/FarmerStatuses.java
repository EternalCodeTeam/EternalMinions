package com.eternalcode.minions.minion.behavior.impl.farmer;

import com.eternalcode.minions.minion.status.MinionStatus;

public final class FarmerStatuses {

    public static final MinionStatus HARVESTING = new MinionStatus("HARVESTING");

    public static final MinionStatus NO_MATURE_CROPS = new MinionStatus("NO_MATURE_CROPS");

    public static final MinionStatus NO_SEEDS = new MinionStatus("NO_SEEDS");

    public static final MinionStatus NO_HOE = new MinionStatus("NO_HOE");

    public static final MinionStatus NO_CONFIGURED_CROPS = new MinionStatus("NO_CONFIGURED_CROPS");

    private FarmerStatuses() {
    }
}