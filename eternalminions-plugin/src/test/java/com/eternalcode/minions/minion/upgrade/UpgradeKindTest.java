package com.eternalcode.minions.minion.upgrade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UpgradeKindTest {

    @Test
    void shouldAcceptUppercaseKeyWithDigitsAndUnderscores() {
        assertThat(new UpgradeKind("ATTACK_RANGE_2").key()).isEqualTo("ATTACK_RANGE_2");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "speed", "Speed", "SPEED-2", "SPEED 2", "SPEED.2"})
    void shouldRejectBlankOrNonUppercaseKeys(String invalidKey) {
        assertThatIllegalArgumentException().isThrownBy(() -> new UpgradeKind(invalidKey));
    }

    @Test
    void shouldRejectNullKey() {
        assertThatIllegalArgumentException().isThrownBy(() -> new UpgradeKind(null));
    }
}
