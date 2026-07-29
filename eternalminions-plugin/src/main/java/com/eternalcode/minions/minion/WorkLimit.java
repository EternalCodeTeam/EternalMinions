package com.eternalcode.minions.minion;

public final class WorkLimit {

    private WorkLimit() {
    }

    public static void validate(String field, int configuredLimit) {
        if (configuredLimit < 0) {
            throw new IllegalArgumentException(field + " cannot be negative: " + configuredLimit);
        }
    }

    public static int resolve(int configuredLimit, int availableWork) {
        validate("work limit", configuredLimit);
        if (availableWork < 0) {
            throw new IllegalArgumentException("Available work cannot be negative: " + availableWork);
        }
        if (configuredLimit == 0) {
            return availableWork;
        }
        return Math.min(configuredLimit, availableWork);
    }
}
