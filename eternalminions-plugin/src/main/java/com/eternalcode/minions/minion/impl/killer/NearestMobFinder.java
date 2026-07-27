package com.eternalcode.minions.minion.impl.killer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
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

            if (!this.isAllowed(
                    livingEntity,
                    allowedMobs,
                    attackAllMonstersWhenEmpty
            )) {
                continue;
            }

            foundAllowedMob = true;

            if (this.isProtected(
                    livingEntity,
                    ignoreNamedMobs,
                    ignoreInvulnerableMobs
            )) {
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

    public List<LivingEntity> findAdditional(
            Location origin,
            Collection<Entity> entities,
            LivingEntity excluded,
            int limit,
            double range,
            Set<EntityType> allowedMobs,
            boolean attackAllMonstersWhenEmpty,
            boolean ignoreNamedMobs,
            boolean ignoreInvulnerableMobs
    ) {
        if (limit <= 0 || range <= 0.0D) {
            return List.of();
        }

        double rangeSquared = range * range;

        List<Candidate> candidates =
                new ArrayList<>();

        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity livingEntity)) {
                continue;
            }

            if (livingEntity.equals(excluded)) {
                continue;
            }

            if (!this.isAllowed(
                    livingEntity,
                    allowedMobs,
                    attackAllMonstersWhenEmpty
            )) {
                continue;
            }

            if (this.isProtected(
                    livingEntity,
                    ignoreNamedMobs,
                    ignoreInvulnerableMobs
            )) {
                continue;
            }

            double distanceSquared =
                    livingEntity.getLocation().distanceSquared(origin);

            if (distanceSquared > rangeSquared) {
                continue;
            }

            candidates.add(
                    new Candidate(
                            livingEntity,
                            distanceSquared
                    )
            );
        }

        candidates.sort(
                Comparator.comparingDouble(
                        Candidate::distanceSquared
                )
        );

        int resultSize = Math.min(
                limit,
                candidates.size()
        );

        List<LivingEntity> result =
                new ArrayList<>(resultSize);

        for (int index = 0; index < resultSize; index++) {
            result.add(
                    candidates.get(index).entity()
            );
        }

        return result;
    }

    private boolean isAllowed(
            LivingEntity entity,
            Set<EntityType> allowedMobs,
            boolean attackAllMonstersWhenEmpty
    ) {
        if (
                entity instanceof Player
                        || !entity.isValid()
                        || entity.isDead()
        ) {
            return false;
        }

        if (!allowedMobs.isEmpty()) {
            return allowedMobs.contains(
                    entity.getType()
            );
        }

        return attackAllMonstersWhenEmpty
                && entity instanceof Monster;
    }

    private boolean isProtected(
            LivingEntity entity,
            boolean ignoreNamedMobs,
            boolean ignoreInvulnerableMobs
    ) {
        return ignoreNamedMobs
                && entity.getCustomName() != null
                || ignoreInvulnerableMobs
                && entity.isInvulnerable();
    }

    private record Candidate(
            LivingEntity entity,
            double distanceSquared
    ) {
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