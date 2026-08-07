package com.eternalcode.minions.minion;

import com.eternalcode.minions.minion.storage.MinionSettings;
import com.eternalcode.minions.minion.storage.MinionStorage;
import com.eternalcode.minions.minion.upgrade.MinionUpgrades;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
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
 * MinionRegistry backs every "is there a minion at this block" and "show minions near this
 * player" check (placement, rendering reconciliation, pickup). Both are chunk-indexed lookups
 * that should stay O(minions-per-chunk), not degrade as the total minion count on a server grows.
 * This benchmarks that claim at minion counts from a small server up to a large modded network.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class MinionRegistrySpatialLookupBenchmark {

    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final int QUERY_POOL_SIZE = 4096;

    @Param({"100", "1000", "5000", "20000"})
    public int minionCount;

    private MinionRegistry registry;
    private int[] queryBlockX;
    private int[] queryBlockZ;
    private int queryIndex;

    @Setup(Level.Trial)
    public void setUp() {
        this.registry = new MinionRegistry();
        Random random = new Random(11);
        // Spread minions over an area sized so that, on average, only a handful share any one
        // chunk - representative of real bases rather than an artificial single-chunk pileup.
        int span = Math.max(64, (int) Math.ceil(Math.sqrt(this.minionCount)) * 16);

        for (long id = 1; id <= this.minionCount; id++) {
            int blockX = random.nextInt(span) - span / 2;
            int blockZ = random.nextInt(span) - span / 2;
            this.registry.register(minionAt(id, blockX, blockZ));
        }

        this.queryBlockX = new int[QUERY_POOL_SIZE];
        this.queryBlockZ = new int[QUERY_POOL_SIZE];
        for (int i = 0; i < QUERY_POOL_SIZE; i++) {
            this.queryBlockX[i] = random.nextInt(span) - span / 2;
            this.queryBlockZ[i] = random.nextInt(span) - span / 2;
        }
    }

    @Benchmark
    public Optional<Minion> findAtRandomPosition() {
        int index = this.queryIndex++ & (QUERY_POOL_SIZE - 1);
        return this.registry.findAt(new MinionPosition("world", this.queryBlockX[index], 64, this.queryBlockZ[index]));
    }

    @Benchmark
    public int countByOwner() {
        return this.registry.countByOwner(OWNER);
    }

    private static Minion minionAt(long id, int blockX, int blockZ) {
        return new Minion(
                new MinionId(id),
                OWNER,
                "MINER",
                new MinionPosition("world", blockX, 64, blockZ),
                MinionProgress.start(),
                MinionEquipment.empty(),
                new MinionStorage(9),
                MinionUpgrades.none(),
                null,
                MinionSettings.defaults()
        );
    }
}
