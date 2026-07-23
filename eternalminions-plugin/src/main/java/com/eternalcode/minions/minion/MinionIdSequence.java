package com.eternalcode.minions.minion;

import java.util.concurrent.atomic.AtomicLong;

public final class MinionIdSequence {

    private final AtomicLong next = new AtomicLong(System.currentTimeMillis());

    public MinionId next() {
        return new MinionId(this.next.incrementAndGet());
    }
}
