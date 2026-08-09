package com.eternalcode.minions.minion.behavior.impl.fisherman;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;

public final class WaterBodyScanner {

    private static final int[] NEIGHBOR_X = {
            1,
            -1,
            0,
            0
    };

    private static final int[] NEIGHBOR_Z = {
            0,
            0,
            1,
            -1
    };

    private static boolean isSourceWater(Block block) {
        if (block.getType() != Material.WATER) {
            return false;
        }

        if (!(block.getBlockData() instanceof Levelled levelled)) {
            return true;
        }

        return levelled.getLevel() == 0;
    }

    private static boolean hasDepth(
            Block surface,
            int minimumDepth
    ) {
        for (int depth = 0; depth < minimumDepth; depth++) {
            Block block = surface.getRelative(0, -depth, 0);

            if (block.getType() != Material.WATER) {
                return false;
            }
        }

        return true;
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    private static int unpackX(long key) {
        return (int) (key >> 32);
    }

    private static int unpackZ(long key) {
        return (int) key;
    }

    public ScanResult scan(
            World world,
            int originX,
            int originY,
            int originZ,
            int searchRange,
            int searchDepth,
            int minimumSurfaceBlocks,
            int minimumDepth,
            boolean requireOpenSurface
    ) {
        Candidate candidate = this.findNearestWater(
                world,
                originX,
                originY,
                originZ,
                searchRange,
                searchDepth,
                minimumDepth,
                requireOpenSurface
        );

        if (candidate == null) {
            return ScanResult.failed(Failure.NO_WATER);
        }

        if (candidate.failure() != null) {
            return ScanResult.failed(candidate.failure());
        }

        Block water = candidate.block();

        int connectedBlocks = this.countSurface(
                world,
                water.getX(),
                water.getY(),
                water.getZ(),
                minimumSurfaceBlocks,
                minimumDepth,
                requireOpenSurface
        );

        if (connectedBlocks < minimumSurfaceBlocks) {
            return ScanResult.failed(Failure.TOO_SMALL);
        }

        return ScanResult.valid(water, connectedBlocks);
    }

    private Candidate findNearestWater(
            World world,
            int originX,
            int originY,
            int originZ,
            int searchRange,
            int searchDepth,
            int minimumDepth,
            boolean requireOpenSurface
    ) {
        Block nearestValid = null;
        int nearestDistance = Integer.MAX_VALUE;

        boolean foundWater = false;
        boolean foundShallowWater = false;
        boolean foundBlockedWater = false;

        for (int offsetY = 0; offsetY >= -searchDepth; offsetY--) {
            int blockY = originY + offsetY;

            for (int offsetX = -searchRange; offsetX <= searchRange; offsetX++) {
                int blockX = originX + offsetX;

                for (int offsetZ = -searchRange; offsetZ <= searchRange; offsetZ++) {
                    int blockZ = originZ + offsetZ;

                    if (!world.isChunkLoaded(blockX >> 4, blockZ >> 4)) {
                        continue;
                    }

                    Block block = world.getBlockAt(
                            blockX,
                            blockY,
                            blockZ
                    );

                    if (!isSourceWater(block)) {
                        continue;
                    }

                    foundWater = true;

                    if (!hasDepth(block, minimumDepth)) {
                        foundShallowWater = true;
                        continue;
                    }

                    if (
                            requireOpenSurface
                                    && !block.getRelative(0, 1, 0).getType().isAir()
                    ) {
                        foundBlockedWater = true;
                        continue;
                    }

                    int distance =
                            offsetX * offsetX
                                    + offsetY * offsetY
                                    + offsetZ * offsetZ;

                    if (distance >= nearestDistance) {
                        continue;
                    }

                    nearestDistance = distance;
                    nearestValid = block;
                }
            }
        }

        if (nearestValid != null) {
            return Candidate.valid(nearestValid);
        }

        if (foundBlockedWater) {
            return Candidate.failed(Failure.SURFACE_BLOCKED);
        }

        if (foundShallowWater) {
            return Candidate.failed(Failure.TOO_SHALLOW);
        }

        if (foundWater) {
            return Candidate.failed(Failure.TOO_SMALL);
        }

        return null;
    }

    private int countSurface(
            World world,
            int startX,
            int waterY,
            int startZ,
            int threshold,
            int minimumDepth,
            boolean requireOpenSurface
    ) {
        LongOpenHashSet visited = new LongOpenHashSet(
                Math.max(16, threshold * 2)
        );

        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();

        long start = key(startX, startZ);

        visited.add(start);
        queue.enqueue(start);

        int count = 0;

        while (!queue.isEmpty()) {
            long current = queue.dequeueLong();

            int currentX = unpackX(current);
            int currentZ = unpackZ(current);

            count++;

            if (count >= threshold) {
                return count;
            }

            for (int index = 0; index < NEIGHBOR_X.length; index++) {
                int neighborX = currentX + NEIGHBOR_X[index];
                int neighborZ = currentZ + NEIGHBOR_Z[index];

                if (!world.isChunkLoaded(neighborX >> 4, neighborZ >> 4)) {
                    continue;
                }

                long neighborKey = key(neighborX, neighborZ);

                if (!visited.add(neighborKey)) {
                    continue;
                }

                Block neighbor = world.getBlockAt(
                        neighborX,
                        waterY,
                        neighborZ
                );

                if (!isSourceWater(neighbor)) {
                    continue;
                }

                if (!hasDepth(neighbor, minimumDepth)) {
                    continue;
                }

                if (
                        requireOpenSurface
                                && !neighbor.getRelative(0, 1, 0).getType().isAir()
                ) {
                    continue;
                }

                queue.enqueue(neighborKey);
            }
        }

        return count;
    }

    public enum Failure {

        NO_WATER,
        TOO_SMALL,
        TOO_SHALLOW,
        SURFACE_BLOCKED
    }

    private record Candidate(
            Block block,
            Failure failure
    ) {

        private static Candidate valid(Block block) {
            return new Candidate(block, null);
        }

        private static Candidate failed(Failure failure) {
            return new Candidate(null, failure);
        }
    }

    public record ScanResult(
            Block water,
            int surfaceBlocks,
            Failure failure
    ) {

        public static ScanResult valid(
                Block water,
                int surfaceBlocks
        ) {
            return new ScanResult(
                    water,
                    surfaceBlocks,
                    null
            );
        }

        public static ScanResult failed(Failure failure) {
            return new ScanResult(
                    null,
                    0,
                    failure
            );
        }

        public boolean valid() {
            return this.water != null;
        }
    }
}