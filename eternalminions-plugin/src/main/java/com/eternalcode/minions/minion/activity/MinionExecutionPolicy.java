package com.eternalcode.minions.minion.activity;

public record MinionExecutionPolicy(boolean storageAllowed) {

    public static final MinionExecutionPolicy FULL = new MinionExecutionPolicy(true);
}
