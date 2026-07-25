package com.eternalcode.minions.minion.status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MinionStatusTest {

    @Test
    void acceptsPlainUppercaseStatusKey() {
        MinionStatus status = new MinionStatus("TOOL_TOO_WEAK");

        assertThat(status.key()).isEqualTo("TOOL_TOO_WEAK");
    }

    @Test
    void rejectsBlankStatusKey() {
        assertThatThrownBy(() -> new MinionStatus(" "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("blank");
    }

    @Test
    void rejectsStatusKeyWithUnsupportedCharacters() {
        assertThatThrownBy(() -> new MinionStatus("Tool Too Weak"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("uppercase");
    }
}
