package com.eternalcode.minions.minion.behavior.impl.miner;

import com.eternalcode.minions.minion.status.MinionStatus;

public final class MinerStatuses {

    public static final MinionStatus MINING = MinionStatus.of("MINING");
    public static final MinionStatus TOOL_TOO_WEAK = MinionStatus.of("TOOL_TOO_WEAK");
    public static final MinionStatus NO_BLOCKS_IN_RANGE = MinionStatus.of("NO_BLOCKS_IN_RANGE");
    public static final MinionStatus NO_PICKAXE = MinionStatus.of("NO_PICKAXE");

    private MinerStatuses() {
    }
}
