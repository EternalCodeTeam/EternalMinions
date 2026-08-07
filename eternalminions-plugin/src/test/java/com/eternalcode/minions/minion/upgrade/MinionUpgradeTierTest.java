package com.eternalcode.minions.minion.upgrade;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MinionUpgradeTierTest {

    @Test
    void shouldRejectNonPositiveRequiredLevel() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new MinionUpgradeTier(0, 10, BigDecimal.TEN));
    }

    @Test
    void shouldRejectNonPositiveValue() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new MinionUpgradeTier(1, 0, BigDecimal.TEN));
    }

    @Test
    void shouldRejectZeroOrNegativeCost() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new MinionUpgradeTier(1, 10, BigDecimal.ZERO));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new MinionUpgradeTier(1, 10, new BigDecimal("-5.00")));
    }

    @Test
    void shouldRejectNullCost() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new MinionUpgradeTier(1, 10, null));
    }
}
