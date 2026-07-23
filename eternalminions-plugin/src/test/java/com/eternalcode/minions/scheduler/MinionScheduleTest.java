package com.eternalcode.minions.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.eternalcode.minions.minion.MinionId;
import org.junit.jupiter.api.Test;

class MinionScheduleTest {

    @Test
    void pollsOnlyDueMinionsInDeadlineOrder() {
        MinionSchedule schedule = new MinionSchedule(2);
        ScheduledMinion late = new ScheduledMinion(new MinionId(1L));
        ScheduledMinion early = new ScheduledMinion(new MinionId(2L));
        schedule.schedule(late, 40L);
        schedule.schedule(early, 10L);

        assertThat(schedule.pollDue(9L)).isNull();
        assertThat(schedule.pollDue(10L)).isSameAs(early);
        assertThat(schedule.pollDue(39L)).isNull();
        assertThat(schedule.pollDue(40L)).isSameAs(late);
    }

    @Test
    void reschedulesExistingMinionWithoutDuplicatingIt() {
        MinionSchedule schedule = new MinionSchedule(1);
        ScheduledMinion minion = new ScheduledMinion(new MinionId(1L));
        schedule.schedule(minion, 20L);

        schedule.schedule(minion, 5L);

        assertThat(schedule.size()).isOne();
        assertThat(schedule.pollDue(5L)).isSameAs(minion);
        assertThat(schedule.size()).isZero();
    }

    @Test
    void cancelsScheduledMinion() {
        MinionSchedule schedule = new MinionSchedule(1);
        ScheduledMinion minion = new ScheduledMinion(new MinionId(1L));
        schedule.schedule(minion, 5L);

        assertThat(schedule.cancel(minion.id())).isTrue();
        assertThat(schedule.pollDue(Long.MAX_VALUE)).isNull();
    }

    @Test
    void advancesMiningTargetCyclically() {
        ScheduledMinion minion = new ScheduledMinion(new MinionId(1L));

        for (int targetIndex = 0; targetIndex < 9; targetIndex++) {
            assertThat(minion.miningTargetIndex()).isEqualTo(targetIndex);
            minion.advanceMiningTarget();
        }

        assertThat(minion.miningTargetIndex()).isZero();
    }
}
