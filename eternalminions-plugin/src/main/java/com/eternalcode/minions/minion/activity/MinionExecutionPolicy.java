package com.eternalcode.minions.minion.activity;

public enum MinionExecutionPolicy {

    FULL(true),
    NO_STORAGE(false);

    private final boolean storageAllowed;

    MinionExecutionPolicy(boolean storageAllowed) {
        this.storageAllowed = storageAllowed;
    }

    public boolean storageAllowed() {
        return this.storageAllowed;
    }
}
