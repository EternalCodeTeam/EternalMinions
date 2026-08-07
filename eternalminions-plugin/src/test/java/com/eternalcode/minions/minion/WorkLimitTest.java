package com.eternalcode.minions.minion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class WorkLimitTest {

    @Test
    void shouldTreatZeroConfiguredLimitAsUnlimited() {
        assertThat(WorkLimit.resolve(0, 500)).isEqualTo(500);
    }

    @Test
    void shouldCapAtConfiguredLimitWhenBelowAvailableWork() {
        assertThat(WorkLimit.resolve(10, 500)).isEqualTo(10);
    }

    @Test
    void shouldReturnAvailableWorkWhenBelowConfiguredLimit() {
        assertThat(WorkLimit.resolve(100, 5)).isEqualTo(5);
    }

    @Test
    void shouldRejectNegativeConfiguredLimit() {
        assertThatIllegalArgumentException().isThrownBy(() -> WorkLimit.resolve(-1, 5));
    }

    @Test
    void shouldRejectNegativeAvailableWork() {
        assertThatIllegalArgumentException().isThrownBy(() -> WorkLimit.resolve(5, -1));
    }
}
