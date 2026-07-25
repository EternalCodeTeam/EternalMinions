package com.eternalcode.minions.minion.killer;

import java.util.List;

// Pure nearest-neighbor search over pre-filtered candidates (allowed-type check already applied
// by the caller — see `Candidate.allowed`) — no Bukkit entity access, so it is directly testable.
public final class NearestMobFinder {

    public record Candidate(int index, double x, double y, double z, boolean allowed) {
    }

    public int findNearest(double originX, double originY, double originZ, List<Candidate> candidates) {
        int bestIndex = -1;
        double bestDistanceSquared = Double.MAX_VALUE;
        for (Candidate candidate : candidates) {
            if (!candidate.allowed()) {
                continue;
            }
            double dx = candidate.x() - originX;
            double dy = candidate.y() - originY;
            double dz = candidate.z() - originZ;
            double distanceSquared = dx * dx + dy * dy + dz * dz;
            if (distanceSquared < bestDistanceSquared) {
                bestDistanceSquared = distanceSquared;
                bestIndex = candidate.index();
            }
        }
        return bestIndex;
    }
}
