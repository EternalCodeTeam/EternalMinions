package com.eternalcode.minions.minion.miner;

import static org.assertj.core.api.Assertions.assertThat;

import com.eternalcode.minions.minion.impl.miner.MinionMiningTargets;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MinionMiningTargetsTest {

    @Test
    void visitsEveryBlockInLayerBeforeWrapping() {
        for (int radius = 1; radius <= MinionMiningTargets.MAX_RADIUS; radius++) {
            int size = 2 * radius + 1;
            Set<String> offsets = new HashSet<>();
            int targetIndex = 0;

            for (int visited = 0; visited < size * size; visited++) {
                offsets.add(
                    MinionMiningTargets.offsetX(radius, targetIndex)
                        + ":" + MinionMiningTargets.offsetZ(radius, targetIndex));
                assertThat(MinionMiningTargets.offsetY()).isEqualTo(-1);
                targetIndex = MinionMiningTargets.nextIndex(radius, targetIndex);
            }

            assertThat(offsets).hasSize(size * size);
            assertThat(targetIndex).isZero();
        }
    }

    @Test
    void providesMinecraftYawForEveryHorizontalDirection() {
        assertThat(MinionMiningTargets.yaw(1, 0)).isEqualTo(135.0F);
        assertThat(MinionMiningTargets.yaw(1, 1)).isEqualTo(180.0F);
        assertThat(MinionMiningTargets.yaw(1, 2)).isEqualTo(-135.0F);
        assertThat(MinionMiningTargets.yaw(1, 3)).isEqualTo(90.0F);
        assertThat(MinionMiningTargets.yaw(1, 4)).isEqualTo(-90.0F);
        assertThat(MinionMiningTargets.yaw(1, 5)).isEqualTo(45.0F);
        assertThat(MinionMiningTargets.yaw(1, 6)).isZero();
        assertThat(MinionMiningTargets.yaw(1, 7)).isEqualTo(-45.0F);
        assertThat(MinionMiningTargets.yaw(1, 8)).isNaN();
    }

    @Test
    void centerColumnIsAlwaysCheckedLast() {
        for (int radius = 1; radius <= MinionMiningTargets.MAX_RADIUS; radius++) {
            int lastIndex = MinionMiningTargets.count(radius) - 1;
            assertThat(MinionMiningTargets.offsetX(radius, lastIndex)).isZero();
            assertThat(MinionMiningTargets.offsetZ(radius, lastIndex)).isZero();
            assertThat(MinionMiningTargets.yaw(radius, lastIndex)).isNaN();
        }
    }
}
