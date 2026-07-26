package com.eternalcode.minions.minion.impl.killer;

import java.util.Collection;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;

public final class NearestMobFinder {

    public SearchResult find(
            Location origin,
            Collection<Entity> entities,
            Set<EntityType> allowedMobs,
            boolean attackAllMonstersWhenEmpty,
            boolean ignoreNamedMobs,
            boolean ignoreInvulnerableMobs
    ) {
        LivingEntity nearest = null;
        double nearestDistanceSquared = Double.MAX_VALUE;

        boolean foundAllowedMob = false;
        boolean foundProtectedMob = false;

        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity livingEntity)) {
                continue;
            }

            if (livingEntity instanceof Player) {
                continue;
            }

            if (!livingEntity.isValid() || livingEntity.isDead()) {
                continue;
            }

            if (
                    !isAllowed(
                            livingEntity,
                            allowedMobs,
                            attackAllMonstersWhenEmpty
                    )
            ) {
                continue;
            }

            foundAllowedMob = true;

            if (
                    ignoreNamedMobs
                            && livingEntity.getCustomName() != null
            ) {
                foundProtectedMob = true;
                continue;
            }

            if (
                    ignoreInvulnerableMobs
                            && livingEntity.isInvulnerable()
            ) {
                foundProtectedMob = true;
                continue;
            }

            double distanceSquared =
                    livingEntity.getLocation().distanceSquared(origin);

            if (distanceSquared >= nearestDistanceSquared) {
                continue;
            }

            nearest = livingEntity;
            nearestDistanceSquared = distanceSquared;
        }

        if (nearest != null) {
            return SearchResult.found(nearest);
        }

        if (foundAllowedMob && foundProtectedMob) {
            return SearchResult.protectedMobs();
        }

        return SearchResult.empty();
    }

    private static boolean isAllowed(
            LivingEntity entity,
            Set<EntityType> allowedMobs,
            boolean attackAllMonstersWhenEmpty
    ) {
        if (!allowedMobs.isEmpty()) {
            return allowedMobs.contains(entity.getType());
        }

        return attackAllMonstersWhenEmpty
                && entity instanceof Monster;
    }

    public record SearchResult(
            LivingEntity target,
            State state
    ) {

        public static SearchResult found(
                LivingEntity target
        ) {
            return new SearchResult(
                    target,
                    State.FOUND
            );
        }

        public static SearchResult protectedMobs() {
            return new SearchResult(
                    null,
                    State.PROTECTED_MOBS
            );
        }

        public static SearchResult empty() {
            return new SearchResult(
                    null,
                    State.EMPTY
            );
        }

        public enum State {

            FOUND,
            PROTECTED_MOBS,
            EMPTY
        }
    }
}