package com.eternalcode.minions.minion.impl.lumberjack;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.bukkit.Material;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

/**
 * The lumberjack minion runs a bounded BFS flood-fill over log/leaf blocks on every work cycle
 * (LumberjackBehavior -> TreeScanner.scan). It's bounded by maxLogs/maxLeaves, but a server admin
 * can configure those limits arbitrarily high, and the branching factor (26 neighbours per block)
 * means a solid trunk core is close to the worst case for how much work one scan can do. This
 * benchmarks that worst case at increasing configured limits to see whether the bound actually
 * keeps per-scan cost predictable.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class TreeScannerBenchmark {

    @Param({"SMALL", "MEDIUM", "LARGE"})
    public TreeSizeParam treeSize;

    private ServerMock server;
    private WorldMock world;
    private TreeScanner scanner;
    private Set<Material> logMaterials;
    private Set<Material> leafMaterials;

    @Setup(Level.Trial)
    public void setUp() {
        this.server = MockBukkit.mock();
        this.world = this.server.addSimpleWorld("world");
        this.scanner = new TreeScanner();
        this.logMaterials = Set.of(Material.OAK_LOG);
        this.leafMaterials = Set.of(Material.OAK_LEAVES);

        int logSide = this.treeSize.logCubeSide;
        int leafPadding = 2;
        int leafSide = logSide + leafPadding * 2;

        // A leaf shell around a solid log core: solid so the log BFS hits close to the maximum
        // branching factor (26 log neighbours) per block, which is the expensive case in
        // practice - a thin single-file trunk barely exercises the neighbour scan at all.
        for (int x = 0; x < leafSide; x++) {
            for (int y = 0; y < leafSide; y++) {
                for (int z = 0; z < leafSide; z++) {
                    this.world.getBlockAt(x, 64 + y, z).setType(Material.OAK_LEAVES);
                }
            }
        }
        for (int x = 0; x < logSide; x++) {
            for (int y = 0; y < logSide; y++) {
                for (int z = 0; z < logSide; z++) {
                    this.world.getBlockAt(
                            x + leafPadding,
                            64 + y + leafPadding,
                            z + leafPadding
                    ).setType(Material.OAK_LOG);
                }
            }
        }
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        MockBukkit.unmock();
    }

    @Benchmark
    public TreeScanner.ScanResult scanTree() {
        int logSide = this.treeSize.logCubeSide;
        int leafPadding = 2;
        return this.scanner.scan(
                this.world,
                leafPadding,
                64 + leafPadding,
                leafPadding,
                this.logMaterials,
                this.leafMaterials,
                this.treeSize.maxLogs,
                this.treeSize.maxLeaves,
                logSide + leafPadding + 2,
                logSide + leafPadding + 2,
                2,
                true,
                true
        );
    }

    public enum TreeSizeParam {
        SMALL(4, 200, 400),
        MEDIUM(8, 2_000, 4_000),
        LARGE(14, 15_000, 20_000);

        final int logCubeSide;
        final int maxLogs;
        final int maxLeaves;

        TreeSizeParam(int logCubeSide, int maxLogs, int maxLeaves) {
            this.logCubeSide = logCubeSide;
            this.maxLogs = maxLogs;
            this.maxLeaves = maxLeaves;
        }
    }
}
