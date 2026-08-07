package com.eternalcode.minions.minion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

class MinionScheduleTest {

    @Test
    void shouldReturnNullWhenNothingIsDue() {
        MinionSchedule schedule = new MinionSchedule(4);
        schedule.schedule(new ScheduledMinion(new MinionId(1)), 10L);

        assertThat(schedule.pollDue(5L)).isNull();
    }

    @Test
    void shouldPollEarliestDeadlineFirst() {
        MinionSchedule schedule = new MinionSchedule(4);
        ScheduledMinion late = new ScheduledMinion(new MinionId(1));
        ScheduledMinion early = new ScheduledMinion(new MinionId(2));
        ScheduledMinion middle = new ScheduledMinion(new MinionId(3));

        schedule.schedule(late, 30L);
        schedule.schedule(early, 10L);
        schedule.schedule(middle, 20L);

        assertThat(schedule.pollDue(100L)).isSameAs(early);
        assertThat(schedule.pollDue(100L)).isSameAs(middle);
        assertThat(schedule.pollDue(100L)).isSameAs(late);
        assertThat(schedule.size()).isZero();
    }

    @Test
    void shouldBreakTiesByAscendingMinionId() {
        MinionSchedule schedule = new MinionSchedule(4);
        ScheduledMinion higherId = new ScheduledMinion(new MinionId(50));
        ScheduledMinion lowerId = new ScheduledMinion(new MinionId(7));

        schedule.schedule(higherId, 10L);
        schedule.schedule(lowerId, 10L);

        assertThat(schedule.pollDue(10L)).isSameAs(lowerId);
        assertThat(schedule.pollDue(10L)).isSameAs(higherId);
    }

    @Test
    void shouldNotPollMinionScheduledAfterCurrentTick() {
        MinionSchedule schedule = new MinionSchedule(4);
        ScheduledMinion minion = new ScheduledMinion(new MinionId(1));
        schedule.schedule(minion, 50L);

        assertThat(schedule.pollDue(49L)).isNull();
        assertThat(schedule.pollDue(50L)).isSameAs(minion);
    }

    @Test
    void shouldRescheduleExistingMinionInPlaceInsteadOfDuplicating() {
        MinionSchedule schedule = new MinionSchedule(4);
        ScheduledMinion minion = new ScheduledMinion(new MinionId(1));

        schedule.schedule(minion, 100L);
        schedule.schedule(minion, 5L);

        assertThat(schedule.size()).isEqualTo(1);
        assertThat(schedule.pollDue(5L)).isSameAs(minion);
        assertThat(schedule.size()).isZero();
    }

    @Test
    void shouldRemoveMinionOnCancelSoItIsNeverPolled() {
        MinionSchedule schedule = new MinionSchedule(4);
        ScheduledMinion minion = new ScheduledMinion(new MinionId(1));
        schedule.schedule(minion, 10L);

        assertThat(schedule.cancel(minion.id())).isTrue();
        assertThat(schedule.pollDue(1000L)).isNull();
        assertThat(schedule.size()).isZero();
    }

    @Test
    void shouldReturnFalseWhenCancellingUnknownMinion() {
        MinionSchedule schedule = new MinionSchedule(4);

        assertThat(schedule.cancel(new MinionId(999))).isFalse();
    }

    @Test
    void shouldGrowBeyondInitialCapacityWithoutLosingEntries() {
        MinionSchedule schedule = new MinionSchedule(2);
        for (long id = 1; id <= 20; id++) {
            schedule.schedule(new ScheduledMinion(new MinionId(id)), id);
        }

        assertThat(schedule.size()).isEqualTo(20);
        for (long expectedId = 1; expectedId <= 20; expectedId++) {
            ScheduledMinion polled = schedule.pollDue(20L);
            assertThat(polled.id()).isEqualTo(new MinionId(expectedId));
        }
    }

    @Test
    void shouldMaintainHeapOrderingUnderManyRandomSchedulesAndReschedules() {
        MinionSchedule schedule = new MinionSchedule(8);
        Random random = new Random(42);
        int minionCount = 500;
        List<ScheduledMinion> minions = new ArrayList<>();
        Map<MinionId, Long> currentDeadlines = new HashMap<>();
        for (long id = 1; id <= minionCount; id++) {
            minions.add(new ScheduledMinion(new MinionId(id)));
        }

        for (ScheduledMinion minion : minions) {
            long deadline = random.nextInt(10_000);
            schedule.schedule(minion, deadline);
            currentDeadlines.put(minion.id(), deadline);
        }
        // Reschedule half of them to new random deadlines, exercising sift-up and sift-down together.
        for (int i = 0; i < minionCount / 2; i++) {
            ScheduledMinion minion = minions.get(random.nextInt(minionCount));
            long deadline = random.nextInt(10_000);
            schedule.schedule(minion, deadline);
            currentDeadlines.put(minion.id(), deadline);
        }

        long lastDeadlinePolled = Long.MIN_VALUE;
        int polledCount = 0;
        ScheduledMinion polled;
        while ((polled = schedule.pollDue(Long.MAX_VALUE)) != null) {
            long deadline = currentDeadlines.get(polled.id());
            assertThat(deadline).isGreaterThanOrEqualTo(lastDeadlinePolled);
            lastDeadlinePolled = deadline;
            polledCount++;
        }

        assertThat(polledCount).isEqualTo(minionCount);
        assertThat(schedule.size()).isZero();
    }

    @Test
    void shouldRejectNonPositiveInitialCapacity() {
        org.assertj.core.api.Assertions.assertThatIllegalArgumentException()
                .isThrownBy(() -> new MinionSchedule(0));
    }
}
