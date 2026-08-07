package com.eternalcode.minions.minion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.eternalcode.minions.config.AbstractMinionConfig;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class MinionProgressTest {

    @Test
    void shouldStartAtLevelOneWithNoProgress() {
        MinionProgress progress = MinionProgress.start();

        assertThat(progress.level()).isEqualTo(1);
        assertThat(progress.progress()).isZero();
    }

    @Test
    void shouldRejectNonPositiveLevel() {
        assertThatIllegalArgumentException().isThrownBy(() -> new MinionProgress(0, 0L));
    }

    @Test
    void shouldRejectNegativeProgress() {
        assertThatIllegalArgumentException().isThrownBy(() -> new MinionProgress(1, -1L));
    }

    @Test
    void shouldAdvanceProgressWithoutLevelingUpBeforeReachingTheThreshold() {
        AbstractMinionConfig config = configWithThresholds(1_000L);
        MinionProgress progress = new MinionProgress(1, 998L);

        MinionProgress advanced = progress.advanced(config);

        assertThat(advanced.level()).isEqualTo(1);
        assertThat(advanced.progress()).isEqualTo(999L);
    }

    @Test
    void shouldLevelUpExactlyWhenReachingTheConfiguredThreshold() {
        AbstractMinionConfig config = configWithThresholds(1_000L);
        MinionProgress progress = new MinionProgress(1, 999L);

        MinionProgress advanced = progress.advanced(config);

        assertThat(advanced.level()).isEqualTo(2);
        assertThat(advanced.progress()).isEqualTo(1_000L);
    }

    @Test
    void shouldLevelUpMultipleTimesInASingleAdvanceWhenThresholdsAreAlreadySatisfied() {
        // A degenerate but reachable config (e.g. after a bad manual edit of levelThresholds)
        // where every remaining threshold is already satisfied by a single +1 progress tick.
        AbstractMinionConfig config = configWithThresholds(1L, 1L, 1L);
        MinionProgress progress = new MinionProgress(1, 0L);

        MinionProgress advanced = progress.advanced(config);

        assertThat(advanced.level()).isEqualTo(4);
        assertThat(advanced.progress()).isEqualTo(1L);
    }

    @Test
    void shouldNeverAdvanceLevelPastConfiguredMaximum() {
        AbstractMinionConfig config = configWithThresholds(1L);
        MinionProgress maxed = new MinionProgress(2, 1L);

        MinionProgress advanced = maxed.advanced(config);

        assertThat(advanced.level()).isEqualTo(2);
        assertThat(advanced.progress()).isEqualTo(2L);
    }

    private static AbstractMinionConfig configWithThresholds(Long... thresholds) {
        AbstractMinionConfig config = new AbstractMinionConfig() {
            @Override
            public Path resolve(Path dataDirectory) {
                return dataDirectory;
            }
        };
        config.levelThresholds = List.of(thresholds);
        return config;
    }
}
