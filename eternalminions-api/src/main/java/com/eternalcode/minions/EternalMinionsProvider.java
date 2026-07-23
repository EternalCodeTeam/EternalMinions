package com.eternalcode.minions;

import java.util.Objects;

public final class EternalMinionsProvider {

    private static EternalMinionsApi api;

    private EternalMinionsProvider() {
    }

    public static EternalMinionsApi provide() {
        if (api == null) {
            throw new IllegalStateException("EternalMinionsApi has not been initialized yet!");
        }

        return api;
    }

    static void initialize(EternalMinionsApi eternalMinionsApi) {
        if (api != null) {
            throw new IllegalStateException("EternalMinionsApi has already been initialized!");
        }

        api = Objects.requireNonNull(eternalMinionsApi, "eternalMinionsApi");
    }

    static void deinitialize() {
        api = null;
    }
}
