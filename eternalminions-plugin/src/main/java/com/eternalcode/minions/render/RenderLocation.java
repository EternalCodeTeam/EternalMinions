package com.eternalcode.minions.render;

import com.eternalcode.minions.minion.MinionPosition;
import com.eternalcode.minions.minion.Minion;
import com.github.retrooper.packetevents.protocol.world.Location;

final class RenderLocation {

    private RenderLocation() {
    }

    static Location of(Minion minion, double yOffset) {
        return of(minion, yOffset, minion.settings().direction().yaw());
    }

    static Location of(Minion minion, double yOffset, float yaw) {
        MinionPosition position = minion.position();
        return new Location(
            position.blockX() + 0.5D,
            position.blockY() + yOffset,
            position.blockZ() + 0.5D,
            yaw,
            0.0F
        );
    }
}
