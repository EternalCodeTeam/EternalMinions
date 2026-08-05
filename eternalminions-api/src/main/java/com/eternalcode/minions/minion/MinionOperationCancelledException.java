package com.eternalcode.minions.minion;

/** Thrown when an administrative operation is cancelled by a public Bukkit event listener. */
public final class MinionOperationCancelledException extends IllegalStateException {

    public MinionOperationCancelledException(String message) {
        super(message);
    }
}
