package com.eternalcode.minions.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;
import org.junit.jupiter.api.Test;

class MinionPanelLayoutTest {

    @Test
    void resolvesRepeatedStorageSymbolsInPatternOrder() {
        MinionPanelConfig config = new MinionPanelConfig();

        MinionPanelLayout layout = MinionPanelLayout.from(config);

        assertThat(layout.slots(MinionPanelAction.STORAGE_SLOT))
            .containsExactly(19, 20, 21, 23, 24, 25, 30, 31, 32);
    }

    @Test
    void rejectsRowsWithInvalidWidth() {
        MinionPanelConfig config = new MinionPanelConfig();
        config.pattern = List.of("########");

        assertThatIllegalArgumentException()
            .isThrownBy(() -> MinionPanelLayout.from(config))
            .withMessage("Minion panel pattern row 1 must contain exactly 9 symbols, found 8");
    }

    @Test
    void rejectsUndefinedPatternSymbol() {
        MinionPanelConfig config = new MinionPanelConfig();
        config.pattern = List.of("####Z####");

        assertThatIllegalArgumentException()
            .isThrownBy(() -> MinionPanelLayout.from(config))
            .withMessage("Minion panel symbol 'Z' has no element definition");
    }

    @Test
    void rejectsMissingPatternWithClearMessage() {
        MinionPanelConfig config = new MinionPanelConfig();
        config.pattern = null;

        assertThatIllegalArgumentException()
            .isThrownBy(() -> MinionPanelLayout.from(config))
            .withMessage("Minion panel pattern is required");
    }

    @Test
    void rejectsMissingElementMaterialWithClearMessage() {
        MinionPanelConfig config = new MinionPanelConfig();
        config.elements.get('T').material = null;

        assertThatIllegalArgumentException()
            .isThrownBy(() -> MinionPanelLayout.from(config))
            .withMessage("Minion panel symbol 'T' material is required");
    }
}
