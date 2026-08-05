package com.eternalcode.minions;

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

        api = eternalMinionsApi;
    }

    static void deinitialize() {
        api = null;
    }
}
