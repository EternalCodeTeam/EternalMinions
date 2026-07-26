package com.eternalcode.minions.minion.upgrade;

// Upgrade kinds interpreted directly by common config scheduling/storage helpers, so every
// profession gets them for free. Professions that want their own upgrade (e.g. attack range) just
// declare a new UpgradeKind constant in their own package - nothing here needs to change.
public final class CoreUpgradeKinds {

    public static final UpgradeKind SPEED = new UpgradeKind("SPEED");
    public static final UpgradeKind RANGE = new UpgradeKind("RANGE");
    public static final UpgradeKind CAPACITY = new UpgradeKind("CAPACITY");

    private CoreUpgradeKinds() {
    }
}
