package com.eternalcode.minions.minion.fisherman;

import static org.assertj.core.api.Assertions.assertThat;

import com.eternalcode.minions.minion.impl.fisherman.WaterBodyScanner;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WaterBodyScannerTest {

    private final WaterBodyScanner scanner = new WaterBodyScanner();

    @Test
    void returnsZeroWhenBaseIsNotWater() {
        assertThat(this.scanner.countConnected(0, 0, 0, (x, y, z) -> false, 5)).isZero();
    }

    @Test
    void countsAllConnectedWaterUpToThreshold() {
        Set<String> pond = Set.of("0,0,0", "1,0,0", "-1,0,0", "0,0,1", "0,0,-1");
        int count = this.scanner.countConnected(0, 0, 0, (x, y, z) -> pond.contains(x + "," + y + "," + z), 10);
        assertThat(count).isEqualTo(5);
    }

    @Test
    void stopsEarlyOnceThresholdIsReached() {
        Set<String> ocean = new HashSet<>();
        for (int x = -20; x <= 20; x++) {
            ocean.add(x + ",0,0");
        }
        int count = this.scanner.countConnected(0, 0, 0, (x, y, z) -> ocean.contains(x + ",0,0"), 8);
        assertThat(count).isEqualTo(8);
    }

    @Test
    void doesNotCrossIntoNonWaterNeighbors() {
        Set<String> puddle = Set.of("0,0,0");
        int count = this.scanner.countConnected(0, 0, 0, (x, y, z) -> puddle.contains(x + "," + y + "," + z), 10);
        assertThat(count).isEqualTo(1);
    }
}
