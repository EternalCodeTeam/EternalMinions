package com.eternalcode.minions.minion.status;

import java.util.regex.Pattern;

public record MinionStatus(String key) {

    private static final Pattern KEY_PATTERN = Pattern.compile("[A-Z0-9_]+");

    public MinionStatus {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Minion status key must not be blank");
        }
        if (!KEY_PATTERN.matcher(key).matches()) {
            throw new IllegalArgumentException("Minion status key must use uppercase letters, numbers and underscores");
        }
    }

    public static MinionStatus of(String key) {
        return new MinionStatus(key);
    }
}
