package com.eternalcode.minions.database;

import com.eternalcode.minions.minion.MinionId;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;

public final class DirtyMinionTracker {

    private final Long2LongOpenHashMap currentVersions = new Long2LongOpenHashMap();
    private final Long2LongOpenHashMap savedVersions = new Long2LongOpenHashMap();

    public long changed(MinionId minionId) {
        long version = this.currentVersions.get(minionId.value()) + 1L;
        this.currentVersions.put(minionId.value(), version);
        return version;
    }

    public long version(MinionId minionId) {
        return this.currentVersions.get(minionId.value());
    }

    public boolean isDirty(MinionId minionId) {
        return this.currentVersions.get(minionId.value()) != this.savedVersions.get(minionId.value());
    }

    public void markSaved(MinionId minionId, long version) {
        this.savedVersions.put(minionId.value(), version);
    }

    public void remove(MinionId minionId) {
        this.currentVersions.remove(minionId.value());
        this.savedVersions.remove(minionId.value());
    }
}
