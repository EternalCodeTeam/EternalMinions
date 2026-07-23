package com.eternalcode.minions.scheduler;

import com.eternalcode.minions.minion.MinionId;

public final class ScheduledMinion {

    private final MinionId id;
    private int miningTargetIndex;

    public ScheduledMinion(MinionId id) {
        this.id = id;
    }

    public MinionId id() {
        return this.id;
    }

    public int miningTargetIndex() {
        return this.miningTargetIndex;
    }

    public void advanceMiningTarget() {
        this.miningTargetIndex = MinionMiningTargets.nextIndex(this.miningTargetIndex);
    }

}
