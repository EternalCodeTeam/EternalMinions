package com.eternalcode.minions;

import org.jetbrains.annotations.NotNull;

/** Global lifecycle-aware accessor for the EternalMinions API implementation. */
public final class EternalMinionsProvider {

    private static EternalMinionsApi api;

    private EternalMinionsProvider() {
    }

    /**
     * Returns the active API instance.
     *
     * @throws IllegalStateException when EternalMinions is not enabled
     */
    public static @NotNull EternalMinionsApi provide() {
        if (api == null) {
            throw new IllegalStateException("EternalMinionsApi has not been initialized yet!");
        }

        return api;
    }

    static void initialize(EternalMinionsApi eternalMinionsApi) {
        if (api != null) {
            throw new IllegalStateException("EternalMinionsApi has already been initialized!");
        }

        api = eternalMinionsApi;
    }

    static void deinitialize() {
        api = null;
    }
}
