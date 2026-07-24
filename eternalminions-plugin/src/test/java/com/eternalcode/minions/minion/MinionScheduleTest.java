package com.eternalcode.minions.minion;

import static org.assertj.core.api.Assertions.assertThat;

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
            assertThat(minion.miningTargetIndex(9)).isEqualTo(targetIndex);
            minion.advanceMiningTarget(9);
        }

        assertThat(minion.miningTargetIndex(9)).isZero();
    }

    @Test
    void clampsMiningTargetWhenRadiusShrinks() {
        ScheduledMinion minion = new ScheduledMinion(new MinionId(1L));
        for (int advanced = 0; advanced < 20; advanced++) {
            minion.advanceMiningTarget(25);
        }

        assertThat(minion.miningTargetIndex(9)).isLessThan(9);
    }
}
