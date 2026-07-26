package com.eternalcode.minions.minion.upgrade;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MinionUpgrades {

    private static final MinionUpgrades NONE = new MinionUpgrades(Map.of());

    private final Map<UpgradeKind, Integer> tiers;

    private MinionUpgrades(Map<UpgradeKind, Integer> tiers) {
        this.tiers = tiers;
    }

    public static MinionUpgrades none() {
        return NONE;
    }

    public int tier(UpgradeKind kind) {
        return this.tiers.getOrDefault(kind, 0);
    }

    public MinionUpgrades withTier(UpgradeKind kind, int tier) {
        if (tier < 0) {
            throw new IllegalArgumentException("Upgrade tier cannot be negative");
        }
        Map<UpgradeKind, Integer> updated = new LinkedHashMap<>(this.tiers);
        if (tier == 0) {
            updated.remove(kind);
        }
        else {
            updated.put(kind, tier);
        }
        return new MinionUpgrades(Map.copyOf(updated));
    }

    // Only kinds with a purchased (non-zero) tier are present, so callers persisting or encoding
    // upgrades never need to enumerate every possible kind - there is no such closed list anymore.
    public Map<UpgradeKind, Integer> entries() {
        return this.tiers;
    }
}
