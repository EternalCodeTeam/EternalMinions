package com.eternalcode.minions.shop;

public interface MinionShopRegistration extends AutoCloseable {

    /**
     * Removes the registered shop provider. Repeated calls have no effect.
     */
    void unregister();

    /**
     * Returns whether the provider is currently registered.
     */
    boolean isRegistered();

    @Override
    default void close() {
        this.unregister();
    }
}
