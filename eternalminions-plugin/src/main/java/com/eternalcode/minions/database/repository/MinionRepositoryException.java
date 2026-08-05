package com.eternalcode.minions.database.repository;

public final class MinionRepositoryException extends RuntimeException {

    public MinionRepositoryException(String message) {
        super(message);
    }

    public MinionRepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
