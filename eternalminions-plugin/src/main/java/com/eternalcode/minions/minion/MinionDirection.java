package com.eternalcode.minions.minion;

public enum MinionDirection {

    SOUTH(0, 1, 0.0F),
    WEST(-1, 0, 90.0F),
    NORTH(0, -1, 180.0F),
    EAST(1, 0, -90.0F);

    private static final MinionDirection[] VALUES = values();

    private final int offsetX;
    private final int offsetZ;
    private final float yaw;

    MinionDirection(int offsetX, int offsetZ, float yaw) {
        this.offsetX = offsetX;
        this.offsetZ = offsetZ;
        this.yaw = yaw;
    }

    public int offsetX() {
        return this.offsetX;
    }

    public int offsetZ() {
        return this.offsetZ;
    }

    public float yaw() {
        return this.yaw;
    }

    public MinionDirection rotated() {
        return VALUES[(this.ordinal() + 1) % VALUES.length];
    }

    public static MinionDirection fromYaw(float yaw) {
        int index = Math.floorMod(Math.round(yaw / 90.0F), 4);
        return switch (index) {
            case 0 -> SOUTH;
            case 1 -> WEST;
            case 2 -> NORTH;
            default -> EAST;
        };
    }
}
