package com.eternalcode.minions.database;

import static org.assertj.core.api.Assertions.assertThat;

import com.eternalcode.minions.minion.MinionId;
import org.junit.jupiter.api.Test;

class DirtyMinionTrackerTest {

    @Test
    void keepsMinionDirtyWhenItChangesDuringSave() {
        DirtyMinionTracker tracker = new DirtyMinionTracker();
        MinionId minionId = new MinionId(7);
        long savedVersion = tracker.changed(minionId);
        tracker.changed(minionId);

        tracker.markSaved(minionId, savedVersion);

        assertThat(tracker.isDirty(minionId)).isTrue();
    }
}
