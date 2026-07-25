package com.eternalcode.minions.minion.fisherman;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

// Pure, bounded breadth-first count of contiguous water blocks connected to a starting point.
// Stops as soon as `threshold` is reached — callers only need "is this at least this big?".
public final class WaterBodyScanner {

    public interface WaterLookup {

        boolean isWater(int x, int y, int z);
    }

    public int countConnected(int baseX, int baseY, int baseZ, WaterLookup lookup, int threshold) {
        if (threshold < 1 || !lookup.isWater(baseX, baseY, baseZ)) {
            return 0;
        }

        Set<Long> visited = new HashSet<>();
        Deque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[] {baseX, baseY, baseZ});
        visited.add(key(baseX, baseY, baseZ));
        int[][] neighbors = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

        int count = 0;
        while (!queue.isEmpty() && count < threshold) {
            int[] current = queue.poll();
            count++;
            if (count >= threshold) {
                break;
            }
            for (int[] offset : neighbors) {
                int nx = current[0] + offset[0];
                int ny = current[1] + offset[1];
                int nz = current[2] + offset[2];
                long neighborKey = key(nx, ny, nz);
                if (visited.contains(neighborKey) || !lookup.isWater(nx, ny, nz)) {
                    continue;
                }
                visited.add(neighborKey);
                queue.add(new int[] {nx, ny, nz});
            }
        }
        return count;
    }

    private static long key(int x, int y, int z) {
        return (((long) x & 0x1FFFFF) << 42) | (((long) y & 0x1FFFFF) << 21) | ((long) z & 0x1FFFFF);
    }
}
