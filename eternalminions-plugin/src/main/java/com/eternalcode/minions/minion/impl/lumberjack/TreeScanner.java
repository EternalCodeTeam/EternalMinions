package com.eternalcode.minions.minion.impl.lumberjack;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Leaves;

public final class TreeScanner {

    private static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38)
                | ((long) (z & 0x3FFFFFF) << 12)
                | (y & 0xFFFL);
    }

    private static int unpackX(long packed) {
        return (int) (packed >> 38);
    }

    private static int unpackY(long packed) {
        int y = (int) (packed & 0xFFFL);

        return y >= 0x800
                ? y - 0x1000
                : y;
    }

    private static int unpackZ(long packed) {
        int z = (int) ((packed >> 12) & 0x3FFFFFFL);

        return z >= 0x2000000
                ? z - 0x4000000
                : z;
    }

    public ScanResult scan(
            World world,
            int baseX,
            int baseY,
            int baseZ,
            Set<Material> logMaterials,
            Set<Material> leafMaterials,
            int maxLogs,
            int maxLeaves,
            int maxTreeRadius,
            int maxTreeHeight,
            int leafSearchRadius,
            boolean collectLeaves,
            boolean collectPersistentLeaves
    ) {
        if (
                maxLogs < 1
                        || baseY < world.getMinHeight()
                        || baseY >= world.getMaxHeight()
                        || !logMaterials.contains(
                        world.getBlockAt(baseX, baseY, baseZ).getType()
                )
        ) {
            return ScanResult.empty();
        }

        PositionBuffer logs = new PositionBuffer(maxLogs);
        LongOpenHashSet visitedLogs = new LongOpenHashSet(
                Math.max(16, maxLogs * 2)
        );

        LongArrayFIFOQueue logQueue =
                new LongArrayFIFOQueue();

        long basePosition = pack(baseX, baseY, baseZ);

        visitedLogs.add(basePosition);
        logQueue.enqueue(basePosition);

        boolean logsTruncated = false;

        while (!logQueue.isEmpty()) {
            long packed = logQueue.dequeueLong();

            int x = unpackX(packed);
            int y = unpackY(packed);
            int z = unpackZ(packed);

            if (!logs.add(x, y, z)) {
                logsTruncated = true;
                break;
            }

            for (int offsetX = -1; offsetX <= 1; offsetX++) {
                for (int offsetY = -1; offsetY <= 1; offsetY++) {
                    for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                        if (
                                offsetX == 0
                                        && offsetY == 0
                                        && offsetZ == 0
                        ) {
                            continue;
                        }

                        int neighborX = x + offsetX;
                        int neighborY = y + offsetY;
                        int neighborZ = z + offsetZ;

                        if (
                                Math.abs(neighborX - baseX) > maxTreeRadius
                                        || Math.abs(neighborZ - baseZ) > maxTreeRadius
                                        || neighborY < baseY - 1
                                        || neighborY > baseY + maxTreeHeight
                        ) {
                            continue;
                        }

                        if (
                                neighborY < world.getMinHeight()
                                        || neighborY >= world.getMaxHeight()
                        ) {
                            continue;
                        }

                        if (
                                !world.isChunkLoaded(
                                        neighborX >> 4,
                                        neighborZ >> 4
                                )
                        ) {
                            continue;
                        }

                        long neighborPosition = pack(
                                neighborX,
                                neighborY,
                                neighborZ
                        );

                        if (!visitedLogs.add(neighborPosition)) {
                            continue;
                        }

                        Material material = world.getBlockAt(
                                neighborX,
                                neighborY,
                                neighborZ
                        ).getType();

                        if (!logMaterials.contains(material)) {
                            continue;
                        }

                        if (logs.size() + logQueue.size() >= maxLogs) {
                            logsTruncated = true;
                            continue;
                        }

                        logQueue.enqueue(neighborPosition);
                    }
                }
            }
        }

        if (logsTruncated) {
            return ScanResult.logsTruncated();
        }

        if (!collectLeaves || leafMaterials.isEmpty()) {
            return ScanResult.complete(
                    logs,
                    PositionBuffer.empty()
            );
        }

        PositionBuffer leaves = new PositionBuffer(maxLeaves);

        LongOpenHashSet visitedLeaves = new LongOpenHashSet(
                Math.max(16, maxLeaves * 2)
        );

        LongArrayFIFOQueue leafQueue =
                new LongArrayFIFOQueue();

        for (int logIndex = 0; logIndex < logs.size(); logIndex++) {
            int logX = logs.x(logIndex);
            int logY = logs.y(logIndex);
            int logZ = logs.z(logIndex);

            for (int offsetX = -1; offsetX <= 1; offsetX++) {
                for (int offsetY = -1; offsetY <= 1; offsetY++) {
                    for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                        if (
                                offsetX == 0
                                        && offsetY == 0
                                        && offsetZ == 0
                        ) {
                            continue;
                        }

                        int leafX = logX + offsetX;
                        int leafY = logY + offsetY;
                        int leafZ = logZ + offsetZ;

                        this.enqueueLeaf(
                                world,
                                leafMaterials,
                                collectPersistentLeaves,
                                baseX,
                                baseY,
                                baseZ,
                                maxTreeRadius,
                                maxTreeHeight,
                                leafSearchRadius,
                                leafX,
                                leafY,
                                leafZ,
                                visitedLeaves,
                                leafQueue
                        );
                    }
                }
            }
        }

        boolean leavesTruncated = false;

        while (!leafQueue.isEmpty()) {
            long packed = leafQueue.dequeueLong();

            int x = unpackX(packed);
            int y = unpackY(packed);
            int z = unpackZ(packed);

            if (!leaves.add(x, y, z)) {
                leavesTruncated = true;
                break;
            }

            for (int offsetX = -1; offsetX <= 1; offsetX++) {
                for (int offsetY = -1; offsetY <= 1; offsetY++) {
                    for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                        if (
                                offsetX == 0
                                        && offsetY == 0
                                        && offsetZ == 0
                        ) {
                            continue;
                        }

                        this.enqueueLeaf(
                                world,
                                leafMaterials,
                                collectPersistentLeaves,
                                baseX,
                                baseY,
                                baseZ,
                                maxTreeRadius,
                                maxTreeHeight,
                                leafSearchRadius,
                                x + offsetX,
                                y + offsetY,
                                z + offsetZ,
                                visitedLeaves,
                                leafQueue
                        );
                    }
                }
            }

            if (leaves.size() + leafQueue.size() > maxLeaves) {
                leavesTruncated = true;
                break;
            }
        }

        if (leavesTruncated) {
            return ScanResult.leavesTruncated();
        }

        return ScanResult.complete(logs, leaves);
    }

    private void enqueueLeaf(
            World world,
            Set<Material> leafMaterials,
            boolean collectPersistentLeaves,
            int baseX,
            int baseY,
            int baseZ,
            int maxTreeRadius,
            int maxTreeHeight,
            int leafSearchRadius,
            int x,
            int y,
            int z,
            LongOpenHashSet visited,
            LongArrayFIFOQueue queue
    ) {
        if (
                y < world.getMinHeight()
                        || y >= world.getMaxHeight()
                        || Math.abs(x - baseX)
                        > maxTreeRadius + leafSearchRadius
                        || Math.abs(z - baseZ)
                        > maxTreeRadius + leafSearchRadius
                        || y < baseY - leafSearchRadius
                        || y > baseY + maxTreeHeight + leafSearchRadius
        ) {
            return;
        }

        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            return;
        }

        long packed = pack(x, y, z);

        if (!visited.add(packed)) {
            return;
        }

        Block block = world.getBlockAt(x, y, z);

        if (!leafMaterials.contains(block.getType())) {
            return;
        }

        if (
                !collectPersistentLeaves
                        && block.getBlockData() instanceof Leaves leaves
                        && leaves.isPersistent()
        ) {
            return;
        }

        queue.enqueue(packed);
    }

    public enum State {

        COMPLETE,
        LOG_LIMIT_REACHED,
        LEAF_LIMIT_REACHED,
        EMPTY
    }

    public record ScanResult(
            PositionBuffer logs,
            PositionBuffer leaves,
            State state
    ) {

        private static ScanResult complete(
                PositionBuffer logs,
                PositionBuffer leaves
        ) {
            return new ScanResult(
                    logs,
                    leaves,
                    State.COMPLETE
            );
        }

        private static ScanResult logsTruncated() {
            return new ScanResult(
                    PositionBuffer.empty(),
                    PositionBuffer.empty(),
                    State.LOG_LIMIT_REACHED
            );
        }

        private static ScanResult leavesTruncated() {
            return new ScanResult(
                    PositionBuffer.empty(),
                    PositionBuffer.empty(),
                    State.LEAF_LIMIT_REACHED
            );
        }

        private static ScanResult empty() {
            return new ScanResult(
                    PositionBuffer.empty(),
                    PositionBuffer.empty(),
                    State.EMPTY
            );
        }

        public boolean complete() {
            return this.state == State.COMPLETE;
        }
    }

    public static final class PositionBuffer {

        private static final PositionBuffer EMPTY =
                new PositionBuffer(0);

        private final int[] positionsX;
        private final int[] positionsY;
        private final int[] positionsZ;

        private int size;

        private PositionBuffer(int capacity) {
            this.positionsX = new int[capacity];
            this.positionsY = new int[capacity];
            this.positionsZ = new int[capacity];
        }

        private static PositionBuffer empty() {
            return EMPTY;
        }

        private boolean add(int x, int y, int z) {
            if (this.size >= this.positionsX.length) {
                return false;
            }

            this.positionsX[this.size] = x;
            this.positionsY[this.size] = y;
            this.positionsZ[this.size] = z;

            this.size++;

            return true;
        }

        public int size() {
            return this.size;
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