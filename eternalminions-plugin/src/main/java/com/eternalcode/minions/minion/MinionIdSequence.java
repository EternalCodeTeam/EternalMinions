package com.eternalcode.minions.minion;

public final class MinionIdSequence {

    private long next = System.currentTimeMillis();

    public MinionId next() {
        return new MinionId(++this.next);
    }
}
