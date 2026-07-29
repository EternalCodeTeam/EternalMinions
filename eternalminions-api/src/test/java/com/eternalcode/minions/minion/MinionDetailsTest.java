package com.eternalcode.minions.minion;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class MinionDetailsTest {

    @Test
    void rejectsBlankBehaviorId() {
        assertThatIllegalArgumentException().isThrownBy(() -> new MinionDetails(
            new MinionId(1L),
            UUID.randomUUID(),
            " ",
            new MinionPosition("world", 0, 64, 0),
            1
        ));
    }

    @Test
    void rejectsNonPositiveLevel() {
        assertThatIllegalArgumentException().isThrownBy(() -> new MinionDetails(
            new MinionId(1L),
            UUID.randomUUID(),
            "miner",
            new MinionPosition("world", 0, 64, 0),
            0
        ));
    }
}
