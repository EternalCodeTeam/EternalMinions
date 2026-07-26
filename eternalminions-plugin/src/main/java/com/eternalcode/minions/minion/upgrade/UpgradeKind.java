package com.eternalcode.minions.minion.upgrade;

import java.util.regex.Pattern;

public record UpgradeKind(String key) {

    private static final Pattern KEY_PATTERN = Pattern.compile("[A-Z0-9_]+");

    public UpgradeKind {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Upgrade kind key must not be blank");
        }
        if (!KEY_PATTERN.matcher(key).matches()) {
            throw new IllegalArgumentException("Upgrade kind key must use uppercase letters, numbers and underscores");
        }
    }
}
