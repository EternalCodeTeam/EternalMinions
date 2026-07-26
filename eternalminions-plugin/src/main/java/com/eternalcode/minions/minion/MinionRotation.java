package com.eternalcode.minions.minion;

public final class MinionRotation {

    private MinionRotation() {
    }

    public static float yawTowards(
            double originX,
            double originZ,
            double targetX,
            double targetZ
    ) {
        double differenceX = targetX - originX;
        double differenceZ = targetZ - originZ;
        return (float) Math.toDegrees(Math.atan2(-differenceX, differenceZ));
    }
}
