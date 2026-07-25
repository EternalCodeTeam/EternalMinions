package com.eternalcode.minions.minion.lumberjack;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.bukkit.Material;
import org.bukkit.World;

public final class TreeScanner {

    private static final int MIN_EXPECTED_POSITIONS = 16;
    private static final int EXPECTED_POSITIONS_PER_LOG = 4;

    public ScanResult scan(
            World world,
            int baseX,
            int baseY,
            int baseZ,
            Material logMaterial,
            int maxLogs
    ) {
        if (maxLogs <= 0) {
            return ScanResult.empty();
        }

        if (baseY < world.getMinHeight() || baseY >= world.getMaxHeight()) {
            return ScanResult.empty();
        }

        if (world.getBlockAt(baseX, baseY, baseZ).getType() != logMaterial) {
            return ScanResult.empty();
        }

        int[] queueX = new int[maxLogs];
        int[] queueY = new int[maxLogs];
        int[] queueZ = new int[maxLogs];

        queueX[0] = baseX;
        queueY[0] = baseY;
        queueZ[0] = baseZ;

        int baseChunkX = baseX >> 4;
        int baseChunkZ = baseZ >> 4;

        int head = 0;
        int tail = 1;

        LongOpenHashSet checkedPositions = new LongOpenHashSet(
                expectedCheckedPositionCount(maxLogs)
        );

        checkedPositions.add(pack(baseX, baseY, baseZ));

        while (head < tail && tail < maxLogs) {
            int x = queueX[head];
            int y = queueY[head];
            int z = queueZ[head];

            head++;

            tail = tryAdd(
                    world,
                    logMaterial,
                    baseChunkX,
                    baseChunkZ,
                    x + 1,
                    y,
                    z,
                    queueX,
                    queueY,
                    queueZ,
                    checkedPositions,
                    tail,
                    maxLogs
            );

            tail = tryAdd(
                    world,
                    logMaterial,
                    baseChunkX,
                    baseChunkZ,
                    x - 1,
                    y,
                    z,
                    queueX,
                    queueY,
                    queueZ,
                    checkedPositions,
                    tail,
                    maxLogs
            );

            tail = tryAdd(
                    world,
                    logMaterial,
                    baseChunkX,
                    baseChunkZ,
                    x,
                    y + 1,
                    z,
                    queueX,
                    queueY,
                    queueZ,
                    checkedPositions,
                    tail,
                    maxLogs
            );

            tail = tryAdd(
                    world,
                    logMaterial,
                    baseChunkX,
                    baseChunkZ,
                    x,
                    y - 1,
                    z,
                    queueX,
                    queueY,
                    queueZ,
                    checkedPositions,
                    tail,
                    maxLogs
            );

            tail = tryAdd(
                    world,
                    logMaterial,
                    baseChunkX,
                    baseChunkZ,
                    x,
                    y,
                    z + 1,
                    queueX,
                    queueY,
                    queueZ,
                    checkedPositions,
                    tail,
                    maxLogs
            );

            tail = tryAdd(
                    world,
                    logMaterial,
                    baseChunkX,
                    baseChunkZ,
                    x,
                    y,
                    z - 1,
                    queueX,
                    queueY,
                    queueZ,
                    checkedPositions,
                    tail,
                    maxLogs
            );
        }

        return new ScanResult(
                queueX,
                queueY,
                queueZ,
                tail
        );
    }

    private static int tryAdd(
            World world,
            Material logMaterial,
            int baseChunkX,
            int baseChunkZ,
            int x,
            int y,
            int z,
            int[] queueX,
            int[] queueY,
            int[] queueZ,
            LongOpenHashSet checkedPositions,
            int tail,
            int maxLogs
    ) {
        if (tail >= maxLogs) {
            return tail;
        }

        if (y < world.getMinHeight() || y >= world.getMaxHeight()) {
            return tail;
        }

        long packedPosition = pack(x, y, z);

        if (!checkedPositions.add(packedPosition)) {
            return tail;
        }

        int chunkX = x >> 4;
        int chunkZ = z >> 4;

        if ((chunkX != baseChunkX || chunkZ != baseChunkZ)
                && !world.isChunkLoaded(chunkX, chunkZ)) {
            return tail;
        }

        if (world.getBlockAt(x, y, z).getType() != logMaterial) {
            return tail;
        }

        queueX[tail] = x;
        queueY[tail] = y;
        queueZ[tail] = z;

        return tail + 1;
    }

    private static int expectedCheckedPositionCount(int maxLogs) {
        long expectedCount = Math.max(
                MIN_EXPECTED_POSITIONS,
                (long) maxLogs * EXPECTED_POSITIONS_PER_LOG
        );

        return (int) Math.min(
                expectedCount,
                Integer.MAX_VALUE - 8L
        );
    }

    private static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38)
                | ((long) (z & 0x3FFFFFF) << 12)
                | (y & 0xFFF);
    }

    public record ScanResult(
            int[] positionsX,
            int[] positionsY,
            int[] positionsZ,
            int size
    ) {

        private static final ScanResult EMPTY = new ScanResult(
                new int[0],
                new int[0],
                new int[0],
                0
        );

        public ScanResult {
            if (size < 0
                    || size > positionsX.length
                    || size > positionsY.length
                    || size > positionsZ.length) {
                throw new IllegalArgumentException(
                        "Tree scan result contains an invalid size"
                );
            }
        }

        public static ScanResult empty() {
            return EMPTY;
        }

        public boolean isEmpty() {
            return this.size == 0;
        }

        public int x(int index) {
            this.checkIndex(index);
            return this.positionsX[index];
        }

        public int y(int index) {
            this.checkIndex(index);
            return this.positionsY[index];
        }

        public int z(int index) {
            this.checkIndex(index);
            return this.positionsZ[index];
        }

        private void checkIndex(int index) {
            if (index < 0 || index >= this.size) {
                throw new IndexOutOfBoundsException(index);
            }
        }
    }
}