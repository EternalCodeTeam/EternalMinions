package com.eternalcode.minions.minion;

import java.util.concurrent.atomic.AtomicLong;

public final class MinionIdSequence {

    // MinionManagementService is public API and can be called by other plugins from arbitrary
    // threads, so a plain long here would let concurrent callers observe a lost update and hand
    // out the same id twice.
    private final AtomicLong next = new AtomicLong(System.currentTimeMillis());

    public MinionId next() {
        return new MinionId(this.next.incrementAndGet());
    }
}
