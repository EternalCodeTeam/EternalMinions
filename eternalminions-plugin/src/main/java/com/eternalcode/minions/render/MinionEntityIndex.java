package com.eternalcode.minions.render;

import com.eternalcode.minions.minion.MinionId;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import java.util.Optional;

public final class MinionEntityIndex {

    private final Int2LongOpenHashMap minionIds = new Int2LongOpenHashMap();

    public MinionEntityIndex() {
        this.minionIds.defaultReturnValue(-1L);
    }

    public void register(int entityId, MinionId minionId) {
        this.minionIds.put(entityId, minionId.value());
    }

    public void remove(int entityId) {
        this.minionIds.remove(entityId);
    }

    public Optional<MinionId> find(int entityId) {
        long minionId = this.minionIds.get(entityId);
        return minionId < 0L ? Optional.empty() : Optional.of(new MinionId(minionId));
    }
}
