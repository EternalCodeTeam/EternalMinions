package com.eternalcode.minions.minion;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class MinionPositionTest {

    @Test
    void rejectsBlankWorldKey() {
        assertThatIllegalArgumentException().isThrownBy(() -> new MinionPosition("", 0, 64, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> new MinionPosition(" ", 0, 64, 0));
    }
}
