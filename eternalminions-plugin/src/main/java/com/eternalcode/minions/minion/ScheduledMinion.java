package com.eternalcode.minions.minion;

public final class ScheduledMinion {

    private final MinionId id;
    private int miningTargetIndex;
    private long busyUntilWorldTime = -1L;
    private long forcedNextDelayTicks = -1L;

    public ScheduledMinion(MinionId id) {
        this.id = id;
    }

    public MinionId id() {
        return this.id;
    }

    // Generic "this minion is mid-action until this world tick" marker, used by professions with
    // a real elapsed-time wait (FISHERMAN's cast-and-reel, KILLER's attack cooldown).
    public boolean isBusyUntil(long worldTime) {
        return worldTime < this.busyUntilWorldTime;
    }

    public void busyUntil(long worldTime) {
        this.busyUntilWorldTime = worldTime;
    }

    // Lets a behavior override the next reschedule delay for one action (e.g. MINER re-polling
    // every tick while mid-dig on a block, instead of waiting the type's full work interval).
    // -1 means "no override, use the type's normal work/idle interval". Consumed (reset to -1)
    // by MinionActionEngine right after each execute() call, so it must be re-set every tick it
    // is still wanted.
    public long forcedNextDelayTicks() {
        return this.forcedNextDelayTicks;
    }

    public void forceNextDelay(long ticks) {
        this.forcedNextDelayTicks = ticks;
    }

    public void clearForcedNextDelay() {
        this.forcedNextDelayTicks = -1L;
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
