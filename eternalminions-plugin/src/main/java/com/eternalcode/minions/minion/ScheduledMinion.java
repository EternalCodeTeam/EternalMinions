package com.eternalcode.minions.minion;

public final class ScheduledMinion {

    private final MinionId id;
    private int miningTargetIndex;

    public ScheduledMinion(MinionId id) {
        this.id = id;
    }

    public MinionId id() {
        return this.id;
    }

    public int miningTargetIndex(int targetCount) {
        // The radius can shrink between actions (config reload), so clamp the cursor into range.
        return this.miningTargetIndex % targetCount;
    }

    public void advanceMiningTarget(int targetCount) {
        this.miningTargetIndex = (this.miningTargetIndex % targetCount) + 1;
        if (this.miningTargetIndex >= targetCount) {
            this.miningTargetIndex = 0;
        }
    }
}
