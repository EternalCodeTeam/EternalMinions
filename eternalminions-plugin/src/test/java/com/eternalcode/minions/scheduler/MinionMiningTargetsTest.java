package com.eternalcode.minions.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MinionMiningTargetsTest {

    @Test
    void visitsEveryBlockInLayerBeforeWrapping() {
        Set<String> offsets = new HashSet<>();
        int targetIndex = 0;

        for (int visited = 0; visited < 9; visited++) {
            offsets.add(MinionMiningTargets.offsetX(targetIndex) + ":" + MinionMiningTargets.offsetZ(targetIndex));
            assertThat(MinionMiningTargets.offsetY()).isEqualTo(-1);
            targetIndex = MinionMiningTargets.nextIndex(targetIndex);
        }

        assertThat(offsets).hasSize(9);
        assertThat(targetIndex).isZero();
    }

    @Test
    void providesMinecraftYawForEveryHorizontalDirection() {
        assertThat(MinionMiningTargets.yaw(0)).isEqualTo(135.0F);
        assertThat(MinionMiningTargets.yaw(1)).isEqualTo(180.0F);
        assertThat(MinionMiningTargets.yaw(2)).isEqualTo(-135.0F);
        assertThat(MinionMiningTargets.yaw(3)).isEqualTo(-90.0F);
        assertThat(MinionMiningTargets.yaw(4)).isEqualTo(-45.0F);
        assertThat(MinionMiningTargets.yaw(5)).isZero();
        assertThat(MinionMiningTargets.yaw(6)).isEqualTo(45.0F);
        assertThat(MinionMiningTargets.yaw(7)).isEqualTo(90.0F);
        assertThat(MinionMiningTargets.yaw(8)).isNaN();
    }
}
