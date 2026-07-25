package com.eternalcode.minions.minion.status;

import com.eternalcode.minions.minion.MinionId;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public final class MinionStatusTracker {

    private final Long2ObjectOpenHashMap<MinionStatus> statuses = new Long2ObjectOpenHashMap<>();
    private final MinionStatus defaultStatus;

    public MinionStatusTracker(MinionStatus defaultStatus) {
        if (defaultStatus == null) {
            throw new IllegalArgumentException("Default minion status must not be null");
        }
        this.defaultStatus = defaultStatus;
    }

    public MinionStatus status(MinionId minionId) {
        if (minionId == null) {
            throw new IllegalArgumentException("Minion id must not be null");
        }

        return this.statuses.getOrDefault(minionId.value(), this.defaultStatus);
    }

    public boolean setStatus(MinionId minionId, MinionStatus status) {
        if (minionId == null) {
            throw new IllegalArgumentException("Minion id must not be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("Minion status must not be null");
        }

        MinionStatus previousStatus = this.status(minionId);
        if (previousStatus.equals(status)) {
            return false;
        }

        this.statuses.put(minionId.value(), status);
        return true;
    }

    public void remove(MinionId minionId) {
        if (minionId == null) {
            throw new IllegalArgumentException("Minion id must not be null");
        }

        this.statuses.remove(minionId.value());
    }
}
