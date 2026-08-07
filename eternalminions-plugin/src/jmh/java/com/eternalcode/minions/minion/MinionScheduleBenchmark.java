package com.eternalcode.minions.minion;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/**
 * MinionActionEngine polls this schedule (a hand-rolled binary heap) once per server tick for
 * every minion coming due, then reschedules it - a scenario that matters at "many minions, 20
 * ticks/second" scale (the docs cite ~500 minions as a realistic upper bound for a busy server).
 * This measures the steady-state cost of one poll+reschedule cycle as the heap size grows, i.e.
 * whether the O(log n) sift-up/down cost stays negligible at the scales a real server can reach.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class MinionScheduleBenchmark {

    @Param({"100", "500", "2000", "5000"})
    public int minionCount;

    private MinionSchedule schedule;
    private long currentTick;

    @Setup(Level.Trial)
    public void setUp() {
        this.schedule = new MinionSchedule(Math.max(16, this.minionCount));
        Random random = new Random(7);
        for (long id = 1; id <= this.minionCount; id++) {
            ScheduledMinion minion = new ScheduledMinion(new MinionId(id));
            this.schedule.schedule(minion, random.nextInt(Math.max(1, this.minionCount)));
        }
        this.currentTick = 0L;
    }

    @Benchmark
    public ScheduledMinion pollAndRescheduleOneDueMinion() {
        this.currentTick++;
        ScheduledMinion due = this.schedule.pollDue(this.currentTick);
        if (due == null) {
            return null;
        }
        // Mirrors the ~40-tick default work interval most behaviors reschedule at.
        this.schedule.schedule(due, this.currentTick + 40L);
        return due;
    }
}
