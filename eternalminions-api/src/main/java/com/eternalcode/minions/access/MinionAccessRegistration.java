package com.eternalcode.minions.access;

public interface MinionAccessRegistration extends AutoCloseable {

    /**
     * Removes the registered access policy. Repeated calls have no effect.
     */
    void unregister();

    /**
     * Returns whether the policy is currently registered.
     */
    boolean isRegistered();

    @Override
    default void close() {
        this.unregister();
    }
}
