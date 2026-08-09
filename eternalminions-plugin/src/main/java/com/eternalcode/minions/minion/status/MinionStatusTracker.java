package com.eternalcode.minions.minion.status;

import com.eternalcode.minions.minion.MinionId;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public final class MinionStatusTracker {

    private final Long2ObjectOpenHashMap<MinionStatus> statuses = new Long2ObjectOpenHashMap<>();

    public MinionStatus status(MinionId minionId) {
        if (minionId == null) {
            throw new IllegalArgumentException("Minion id must not be null");
        }

        return this.statuses.getOrDefault(minionId.value(), CoreMinionStatuses.IDLE);
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

        if (status.equals(CoreMinionStatuses.IDLE)) {
            this.statuses.remove(minionId.value());
            return true;
        }

        this.statuses.put(minionId.value(), status);
        return true;
    }

    public boolean clearStatus(MinionId minionId) {
        if (minionId == null) {
            throw new IllegalArgumentException("Minion id must not be null");
        }

        return this.statuses.remove(minionId.value()) != null;
    }
}
