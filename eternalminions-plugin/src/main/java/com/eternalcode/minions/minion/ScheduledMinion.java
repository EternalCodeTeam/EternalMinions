package com.eternalcode.minions.minion;

public final class ScheduledMinion {

    private static final long NO_BUSY_TIMER = -1L;

    private final MinionId id;

    private int miningTargetIndex;
    private long busyUntilWorldTime = NO_BUSY_TIMER;
    private float animationYaw = Float.NaN;

    public ScheduledMinion(MinionId id) {
        this.id = id;
    }

    public MinionId id() {
        return this.id;
    }

    public boolean hasBusyTimer() {
        return this.busyUntilWorldTime != NO_BUSY_TIMER;
    }

    public boolean isBusyUntil(long worldTime) {
        return this.hasBusyTimer()
                && worldTime < this.busyUntilWorldTime;
    }

    public long remainingBusyTicks(long worldTime) {
        if (!this.hasBusyTimer()) {
            return 0L;
        }

        return Math.max(
                0L,
                this.busyUntilWorldTime - worldTime
        );
    }

    public void busyUntil(long worldTime) {
        this.busyUntilWorldTime = worldTime;
    }

    public void clearBusyTimer() {
        this.busyUntilWorldTime = NO_BUSY_TIMER;
    }

    public void face(float yaw) {
        this.animationYaw = yaw;
    }

    public float consumeAnimationYaw() {
        float yaw = this.animationYaw;

        this.animationYaw = Float.NaN;

        return yaw;
    }

    public int miningTargetIndex(int targetCount) {
        if (targetCount <= 0) {
            return 0;
        }

        return Math.floorMod(
                this.miningTargetIndex,
                targetCount
        );
    }

    public void advanceMiningTarget(int targetCount) {
        if (targetCount <= 0) {
            this.miningTargetIndex = 0;
            return;
        }

        this.miningTargetIndex = (this.miningTargetIndex + 1) % targetCount;
    }
}

