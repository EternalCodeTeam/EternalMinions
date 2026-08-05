package com.eternalcode.minions.minion;

/** Cardinal direction in which a minion faces and performs directional work. */
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

    /** Returns the X block offset associated with this direction. */
    public int offsetX() {
        return this.offsetX;
    }

    /** Returns the Z block offset associated with this direction. */
    public int offsetZ() {
        return this.offsetZ;
    }

    /** Returns the Bukkit yaw associated with this direction. */
    public float yaw() {
        return this.yaw;
    }

    /** Returns the next clockwise direction. */
    public MinionDirection rotated() {
        return VALUES[(this.ordinal() + 1) % VALUES.length];
    }

    /** Resolves the closest cardinal direction for a Bukkit yaw. */
    public static MinionDirection fromYaw(float yaw) {
        int index = Math.floorMod(Math.round(yaw / 90.0F), VALUES.length);
        return VALUES[index];
    }
}
