package com.eternalcode.minions.minion.killer;

import java.util.EnumSet;
import java.util.Set;
import org.bukkit.entity.EntityType;

// KILLER-type tuning: which mobs it may attack, how far it can reach, how long between swings
// (configurable rather than derived from a live weapon-attribute read, since minions render as
// packet-only bodies and have no server-side attack-speed attribute to read), and the weapon's
// base damage before enchantment bonuses.
public record KillerWork(Set<EntityType> allowedMobs, int attackRangeBlocks, int attackCooldownTicks, double baseDamage) {

    public KillerWork {
        if (allowedMobs == null || allowedMobs.isEmpty()) {
            throw new IllegalArgumentException("Killer work requires at least one allowed mob type");
        }
        if (attackRangeBlocks < 1) {
            throw new IllegalArgumentException("Killer work attack range must be positive");
        }
        if (attackCooldownTicks < 1) {
            throw new IllegalArgumentException("Killer work attack cooldown must be positive");
        }
        if (baseDamage <= 0.0D) {
            throw new IllegalArgumentException("Killer work base damage must be positive");
        }
        allowedMobs = EnumSet.copyOf(allowedMobs);
    }
}
