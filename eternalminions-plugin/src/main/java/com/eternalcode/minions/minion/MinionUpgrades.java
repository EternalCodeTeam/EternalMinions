package com.eternalcode.minions.minion;

public final class MinionUpgrades {

    private static final MinionUpgradeKind[] KINDS = MinionUpgradeKind.values();
    private static final MinionUpgrades NONE = new MinionUpgrades(new int[KINDS.length]);

    private final int[] tiers;

    private MinionUpgrades(int[] tiers) {
        this.tiers = tiers;
    }

    public static MinionUpgrades none() {
        return NONE;
    }

    public int tier(MinionUpgradeKind kind) {
        return this.tiers[kind.ordinal()];
    }

    public MinionUpgrades withTier(MinionUpgradeKind kind, int tier) {
        if (tier < 0) {
            throw new IllegalArgumentException("Upgrade tier cannot be negative");
        }
        int[] updated = this.tiers.clone();
        updated[kind.ordinal()] = tier;
        return new MinionUpgrades(updated);
    }
}
