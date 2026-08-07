package com.eternalcode.minions.minion.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.eternalcode.minions.minion.activity.config.ActivityMode;
import com.eternalcode.minions.minion.status.MinionStatus;
import org.junit.jupiter.api.Test;

class ActivityDecisionTest {

    private static final MinionStatus FROZEN_STATUS = new MinionStatus("OWNER_OFFLINE");
    private static final MinionStatus SLOW_STATUS = new MinionStatus("CHUNK_UNLOADED");
    private static final MinionStatus OTHER_SLOW_STATUS = new MinionStatus("NO_NEARBY_PLAYER");

    @Test
    void shouldRejectNonFiniteOrNonPositiveSpeedMultiplier() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ActivityDecision(false, 0.0D, false, null));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ActivityDecision(false, -1.0D, false, null));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ActivityDecision(false, Double.NaN, false, null));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ActivityDecision(false, Double.POSITIVE_INFINITY, false, null));
    }

    @Test
    void shouldClampSpeedMultiplierAboveOneToOne() {
        ActivityDecision decision = new ActivityDecision(false, 2.5D, false, null);

        assertThat(decision.speedMultiplier()).isEqualTo(1.0D);
    }

    @Test
    void shouldTreatFullSpeedWithoutSuppressionAsActive() {
        assertThat(ActivityDecision.ACTIVE.active()).isTrue();
    }

    @Test
    void shouldNotBeActiveWhenStorageGainIsSuppressedEvenAtFullSpeed() {
        ActivityDecision decision = ActivityDecision.of(ActivityMode.SLOW_NO_STORAGE, 1.0D, SLOW_STATUS);

        assertThat(decision.speedMultiplier()).isEqualTo(1.0D);
        assertThat(decision.suppressStorageGain()).isTrue();
        assertThat(decision.active()).isFalse();
    }

    @Test
    void shouldFreezeAndSuppressStorageWhenModeIsFreeze() {
        ActivityDecision decision = ActivityDecision.of(ActivityMode.FREEZE, 0.5D, FROZEN_STATUS);

        assertThat(decision.frozen()).isTrue();
        assertThat(decision.suppressStorageGain()).isTrue();
        assertThat(decision.statusOverride()).isEqualTo(FROZEN_STATUS);
    }

    @Test
    void shouldReturnFrozenDecisionRegardlessOfWhichSideIsFrozen() {
        ActivityDecision frozen = ActivityDecision.frozen(FROZEN_STATUS);
        ActivityDecision slow = ActivityDecision.of(ActivityMode.SLOW, 0.5D, SLOW_STATUS);

        assertThat(ActivityDecision.moreSevere(frozen, slow)).isEqualTo(frozen);
        assertThat(ActivityDecision.moreSevere(slow, frozen)).isEqualTo(frozen);
    }

    @Test
    void shouldPreferTheRestrictiveSideWhenOnlyOneRuleIsRestrictive() {
        ActivityDecision restrictive = ActivityDecision.of(ActivityMode.SLOW, 0.4D, SLOW_STATUS);

        assertThat(ActivityDecision.moreSevere(ActivityDecision.ACTIVE, restrictive)).isEqualTo(restrictive);
        assertThat(ActivityDecision.moreSevere(restrictive, ActivityDecision.ACTIVE)).isEqualTo(restrictive);
    }

    @Test
    void shouldCombineTwoRestrictiveRulesByTakingTheSlowerMultiplierAndOrringSuppression() {
        ActivityDecision slower = ActivityDecision.of(ActivityMode.SLOW, 0.3D, SLOW_STATUS);
        ActivityDecision fasterButSuppressing = ActivityDecision.of(ActivityMode.SLOW_NO_STORAGE, 0.8D, OTHER_SLOW_STATUS);

        ActivityDecision combined = ActivityDecision.moreSevere(slower, fasterButSuppressing);

        assertThat(combined.speedMultiplier()).isEqualTo(0.3D);
        assertThat(combined.suppressStorageGain())
                .as("suppression from either contributing rule must carry over even though it wasn't the slower one")
                .isTrue();
    }

    /**
     * Documents a real quirk found in moreSevere(): at equal speed multipliers, the status shown
     * to the player always comes from the first-evaluated rule, even when the second rule is the
     * one actually responsible for suppressing storage gain. This pins current behavior so a
     * future refactor changes it deliberately instead of by accident.
     */
    @Test
    void shouldKeepFirstRuleStatusEvenWhenSecondRuleAtEqualSpeedCausesSuppression() {
        ActivityDecision first = ActivityDecision.of(ActivityMode.SLOW, 0.5D, SLOW_STATUS);
        ActivityDecision second = ActivityDecision.of(ActivityMode.SLOW_NO_STORAGE, 0.5D, OTHER_SLOW_STATUS);

        ActivityDecision combined = ActivityDecision.moreSevere(first, second);

        assertThat(combined.suppressStorageGain()).isTrue();
        assertThat(combined.statusOverride()).isEqualTo(SLOW_STATUS);
    }
}
