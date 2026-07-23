package com.eternalcode.minions.minion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class MinionIdTest {

    @Test
    void acceptsPositiveValue() {
        assertThat(new MinionId(42L).value()).isEqualTo(42L);
    }

    @Test
    void rejectsNonPositiveValue() {
        assertThatIllegalArgumentException().isThrownBy(() -> new MinionId(0L));
        assertThatIllegalArgumentException().isThrownBy(() -> new MinionId(-1L));
    }
}
