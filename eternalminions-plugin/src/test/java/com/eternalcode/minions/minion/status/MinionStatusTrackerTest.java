package com.eternalcode.minions.minion.status;

import static org.assertj.core.api.Assertions.assertThat;

import com.eternalcode.minions.minion.MinionId;
import org.junit.jupiter.api.Test;

class MinionStatusTrackerTest {

    private final MinionStatusTracker tracker = new MinionStatusTracker(CoreMinionStatuses.IDLE);

    @Test
    void returnsDefaultStatusWhenMinionHasNoRuntimeStatus() {
        assertThat(this.tracker.status(new MinionId(1L))).isEqualTo(CoreMinionStatuses.IDLE);
    }

    @Test
    void reportsChangedOnlyWhenStatusActuallyChanges() {
        MinionId minionId = new MinionId(1L);

        assertThat(this.tracker.setStatus(minionId, CoreMinionStatuses.WORKING)).isTrue();
        assertThat(this.tracker.setStatus(minionId, CoreMinionStatuses.WORKING)).isFalse();
        assertThat(this.tracker.status(minionId)).isEqualTo(CoreMinionStatuses.WORKING);
    }

    @Test
    void forgetsStatusWhenMinionIsRemoved() {
        MinionId minionId = new MinionId(1L);

        this.tracker.setStatus(minionId, CoreMinionStatuses.WORKING);
        this.tracker.remove(minionId);

        assertThat(this.tracker.status(minionId)).isEqualTo(CoreMinionStatuses.IDLE);
    }
}
