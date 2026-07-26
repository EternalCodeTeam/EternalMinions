package com.eternalcode.minions.minion;

public final class ScheduledMinion {

    private final MinionId id;
    private int miningTargetIndex;
    private long busyUntilWorldTime = -1L;
    private float animationYaw = Float.NaN;

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

    public boolean hasBusyTimer() {
        return this.busyUntilWorldTime >= 0L;
    }

    public long remainingBusyTicks(long worldTime) {
        return Math.max(1L, this.busyUntilWorldTime - worldTime);
    }

    public void clearBusyTimer() {
        this.busyUntilWorldTime = -1L;
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
