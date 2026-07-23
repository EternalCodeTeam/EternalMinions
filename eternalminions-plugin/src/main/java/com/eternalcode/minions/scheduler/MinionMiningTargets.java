package com.eternalcode.minions.scheduler;

final class MinionMiningTargets {

    private static final int[] X_OFFSETS = {-1, 0, 1, 1, 1, 0, -1, -1, 0};
    private static final int[] Z_OFFSETS = {-1, -1, -1, 0, 1, 1, 1, 0, 0};
    private static final float[] YAWS = {135.0F, 180.0F, -135.0F, -90.0F, -45.0F, 0.0F, 45.0F, 90.0F, Float.NaN};
    private static final int Y_OFFSET = -1;

    private MinionMiningTargets() {
    }

    static int offsetX(int targetIndex) {
        return X_OFFSETS[targetIndex];
    }

    static int offsetY() {
        return Y_OFFSET;
    }

    static int offsetZ(int targetIndex) {
        return Z_OFFSETS[targetIndex];
    }

    static float yaw(int targetIndex) {
        return YAWS[targetIndex];
    }

    static int nextIndex(int targetIndex) {
        int nextIndex = targetIndex + 1;
        return nextIndex == X_OFFSETS.length ? 0 : nextIndex;
    }

    static int count() {
        return X_OFFSETS.length;
    }
}
