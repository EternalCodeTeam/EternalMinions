package com.eternalcode.minions.minion.impl.miner;

final class MinionMiningTargets {

    static final int MAX_RADIUS = 3;

    private static final int[][] X_OFFSETS = new int[MAX_RADIUS][];
    private static final int[][] Z_OFFSETS = new int[MAX_RADIUS][];
    private static final float[][] YAWS = new float[MAX_RADIUS][];
    private static final int Y_OFFSET = -1;

    static {
        for (int radius = 1; radius <= MAX_RADIUS; radius++) {
            int size = 2 * radius + 1;
            int count = size * size;
            int[] xOffsets = new int[count];
            int[] zOffsets = new int[count];
            float[] yaws = new float[count];

            // Ring cells first, center last, so the minion sweeps around itself before its own column.
            int index = 0;
            for (int offsetZ = -radius; offsetZ <= radius; offsetZ++) {
                for (int offsetX = -radius; offsetX <= radius; offsetX++) {
                    if (offsetX == 0 && offsetZ == 0) {
                        continue;
                    }
                    xOffsets[index] = offsetX;
                    zOffsets[index] = offsetZ;
                    yaws[index] = (float) Math.toDegrees(Math.atan2(-offsetX, offsetZ));
                    index++;
                }
            }
            xOffsets[index] = 0;
            zOffsets[index] = 0;
            yaws[index] = Float.NaN;

            X_OFFSETS[radius - 1] = xOffsets;
            Z_OFFSETS[radius - 1] = zOffsets;
            YAWS[radius - 1] = yaws;
        }
    }

    private MinionMiningTargets() {
    }

    static int offsetX(int radius, int targetIndex) {
        return X_OFFSETS[radius - 1][targetIndex];
    }

    static int offsetY() {
        return Y_OFFSET;
    }

    static int offsetZ(int radius, int targetIndex) {
        return Z_OFFSETS[radius - 1][targetIndex];
    }

    static float yaw(int radius, int targetIndex) {
        return YAWS[radius - 1][targetIndex];
    }

    static int nextIndex(int radius, int targetIndex) {
        int nextIndex = targetIndex + 1;
        return nextIndex >= count(radius) ? 0 : nextIndex;
    }

    static int count(int radius) {
        return X_OFFSETS[radius - 1].length;
    }
}
