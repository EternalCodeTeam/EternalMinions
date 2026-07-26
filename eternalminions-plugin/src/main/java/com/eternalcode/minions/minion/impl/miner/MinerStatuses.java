package com.eternalcode.minions.minion.impl.miner;

import com.eternalcode.minions.minion.status.MinionStatus;

public final class MinerStatuses {

    public static final MinionStatus MINING = new MinionStatus("MINING");
    public static final MinionStatus TOOL_TOO_WEAK = new MinionStatus("TOOL_TOO_WEAK");
    public static final MinionStatus NO_BLOCKS_IN_RANGE = new MinionStatus("NO_BLOCKS_IN_RANGE");
    public static final MinionStatus NO_PICKAXE = new MinionStatus("NO_PICKAXE");

    private MinerStatuses() {
    }
}
