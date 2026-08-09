package com.eternalcode.minions.minion.behavior.impl.lumberjack;

import com.cryptomorin.xseries.XMaterial;
import com.eternalcode.minions.config.AbstractMinionConfig;
import com.eternalcode.minions.config.ConfigService;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.behavior.MinionBehavior;
import com.eternalcode.minions.minion.behavior.BlockDrops;
import com.eternalcode.minions.minion.MinionDirection;
import com.eternalcode.minions.minion.behavior.MinionContext;
import com.eternalcode.minions.minion.behavior.MinionResult;
import com.eternalcode.minions.minion.storage.MinionItemTransferService;
import com.eternalcode.minions.minion.status.CoreMinionStatuses;
import com.eternalcode.minions.minion.status.MinionStatus;
import com.eternalcode.minions.minion.tool.MinionToolPreparation;
import com.eternalcode.minions.minion.tool.MinionToolService;
import com.eternalcode.minions.minion.tool.ToolCheck;
import com.eternalcode.minions.minion.tool.ToolRequirement;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

public final class LumberjackBehavior implements MinionBehavior {

    private static final int EXPECTED_DROPS_PER_BLOCK = 2;

    private final LumberjackConfig config;

    private final MinionToolService tools;
    private final MinionItemTransferService transfers;
    private final ToolRequirement toolRequirement;

    private final TreeScanner treeScanner = new TreeScanner();

    private final Set<Material> logMaterials;
    private final Set<Material> leafMaterials;
    private final Map<Material, Material> saplingByLog;
    private final Set<Material> saplingMaterials;
    private final Material defaultSapling;

    public static LumberjackBehavior create(
            ConfigService configs,
            MinionToolService tools,
            MinionItemTransferService transfers
    ) {
        LumberjackConfig config = configs.get(LumberjackConfig.class);

        return new LumberjackBehavior(
                config,
                tools,
                transfers
        );
    }

    public LumberjackBehavior(
            LumberjackConfig config,
            MinionToolService tools,
            MinionItemTransferService transfers
    ) {
        this.config = config;

        this.tools = tools;
        this.transfers = transfers;
        this.toolRequirement = config.toolRequirement();

        this.logMaterials = parseMaterials(
                config.logMaterials,
                "logs"
        );

        this.leafMaterials = parseOptionalMaterials(
                config.leafMaterials
        );

        this.saplingByLog = parseSaplingByLog(
                config.saplingByLog
        );

        this.saplingMaterials = Set.copyOf(this.saplingByLog.values());

        this.defaultSapling = this.saplingMaterials.isEmpty()
                ? requireMaterial(XMaterial.OAK_SAPLING, "sapling")
                : this.saplingMaterials.iterator().next();
        if (config.maxTreesPerCycle < 0) {
            throw new IllegalArgumentException(
                    "lumberjack.maxTreesPerCycle cannot be negative: " + config.maxTreesPerCycle
            );
        }
    }

    @Override
    public String id() {
        return "lumberjack";
    }

    @Override
    public AbstractMinionConfig config() {
        return this.config;
    }

    @Override
    public MinionResult execute(MinionContext context) {
        MinionToolPreparation preparation = this.tools.prepare(
                context,
                this.toolRequirement,
                LumberjackStatuses.NO_AXE
        );
        if (preparation.check() instanceof ToolCheck.Stopped stopped) {
            return MinionResult.idle(
                    preparation.minion(),
                    stopped.reason()
            );
        }
        Minion minion = preparation.minion();

        int stationCount = this.config.stationCount(
                minion.upgrades()
        );

        MinionDirection direction =
                minion.settings().direction();

        int firstStationIndex = context.scheduledMinion()
                .miningTargetIndex(stationCount);

        context.scheduledMinion()
                .advanceMiningTarget(stationCount);

        boolean foundSapling = false;
        boolean foundInvalidStation = false;
        int treeLimit = this.config.maxTreesPerCycle == 0
                ? stationCount
                : Math.min(this.config.maxTreesPerCycle, stationCount);
        int felledTrees = 0;
        Minion updated = minion;

        for (int offset = 0; offset < stationCount; offset++) {
            int stationIndex =
                    (firstStationIndex + offset) % stationCount;

            int distance = stationIndex + 1;

            int stationX =
                    updated.position().blockX()
                            + direction.offsetX() * distance;

            int stationY =
                    updated.position().blockY();

            int stationZ =
                    updated.position().blockZ()
                            + direction.offsetZ() * distance;

            if (
                    !context.world().isChunkLoaded(
                            stationX >> 4,
                            stationZ >> 4
                    )
            ) {
                continue;
            }

            Block station = context.world().getBlockAt(
                    stationX,
                    stationY,
                    stationZ
            );

            Material material = station.getType();

            if (this.logMaterials.contains(material)) {
                context.scheduledMinion().face(
                        direction.yaw()
                );

                MinionResult treeResult = this.fellTree(
                        context,
                        updated,
                        station
                );
                if (!treeResult.worked()) {
                    if (felledTrees == 0) {
                        return treeResult;
                    }
                    break;
                }
                updated = treeResult.minion();
                felledTrees++;
                if (felledTrees >= treeLimit) {
                    break;
                }
                continue;
            }

            if (this.saplingMaterials.contains(material)) {
                foundSapling = true;
                continue;
            }

            if (!material.isAir()) {
                foundInvalidStation = true;
            }
        }

        if (felledTrees > 0) {
            return MinionResult.worked(updated, LumberjackStatuses.CUTTING);
        }
        return MinionResult.idle(
                updated,
                resolveIdleStatus(
                        foundSapling,
                        foundInvalidStation
                )
        );
    }

    private MinionResult fellTree(
            MinionContext context,
            Minion minion,
            Block trunkBase
    ) {
        TreeScanner.ScanResult tree =
                this.treeScanner.scan(
                        context.world(),
                        trunkBase.getX(),
                        trunkBase.getY(),
                        trunkBase.getZ(),
                        this.logMaterials,
                        this.leafMaterials,
                        this.config.maximumLogs(),
                        this.config.maximumLeaves(),
                        this.config.maximumTreeRadius(),
                        this.config.maximumTreeHeight(),
                        this.config.maximumLeafRadius(),
                        this.config.breakLeaves,
                        this.config.breakPersistentLeaves
                );

        if (
                tree.state()
                        == TreeScanner.State.LOG_LIMIT_REACHED
        ) {
            return MinionResult.idle(
                    minion,
                    LumberjackStatuses.TREE_TOO_LARGE
            );
        }

        if (
                tree.state()
                        == TreeScanner.State.LEAF_LIMIT_REACHED
        ) {
            return MinionResult.idle(
                    minion,
                    LumberjackStatuses.CANOPY_TOO_LARGE
            );
        }

        if (!tree.complete() || tree.logs().size() == 0) {
            return MinionResult.idle(
                    minion,
                    LumberjackStatuses.INVALID_STATION
            );
        }

        Material sapling = this.saplingByLog.getOrDefault(
                trunkBase.getType(),
                this.defaultSapling
        );

        ItemStack tool = minion.equipment().tool();

        int expectedBlocks =
                tree.logs().size()
                        + tree.leaves().size()
                        + tree.hives().size();

        List<ItemStack> drops = new ArrayList<>(
                expectedDropCapacity(expectedBlocks)
        );

        this.collectDrops(
                context,
                tree.logs(),
                tool,
                drops
        );

        this.collectDrops(
                context,
                tree.leaves(),
                tool,
                drops
        );

        this.collectDrops(
                context,
                tree.hives(),
                tool,
                drops
        );

        if (!this.transfers.canStoreAll(context, minion.storage(), drops)) {
            return MinionResult.idle(minion, CoreMinionStatuses.STORAGE_FULL);
        }

        this.breakBlocks(
                context,
                tree.hives()
        );

        this.breakBlocks(
                context,
                tree.leaves()
        );

        this.breakBlocks(
                context,
                tree.logs()
        );

        this.replant(
                context,
                tree.logs(),
                trunkBase.getY(),
                sapling
        );

        Minion updated = this.tools.consume(minion, tree.logs().size());

        updated = this.transfers.deposit(
                context,
                updated,
                trunkBase.getLocation(),
                drops
        );

        updated = updated.withProgress(
                updated.progress().advanced(this.config)
        );

        return MinionResult.worked(
                updated,
                LumberjackStatuses.CUTTING
        );
    }

    private void collectDrops(
            MinionContext context,
            TreeScanner.PositionBuffer blocks,
            ItemStack tool,
            List<ItemStack> destination
    ) {
        for (int index = 0; index < blocks.size(); index++) {
            Block block = context.world().getBlockAt(
                    blocks.x(index),
                    blocks.y(index),
                    blocks.z(index)
            );

            BlockDrops.collectInto(block, tool, destination);
        }
    }

    private void breakBlocks(
            MinionContext context,
            TreeScanner.PositionBuffer blocks
    ) {
        for (int index = 0; index < blocks.size(); index++) {
            Block block = context.world().getBlockAt(
                    blocks.x(index),
                    blocks.y(index),
                    blocks.z(index)
            );

            block.setType(
                    Material.AIR,
                    false
            );
        }
    }

    private void replant(
            MinionContext context,
            TreeScanner.PositionBuffer logs,
            int baseY,
            Material sapling
    ) {
        if (!this.config.replantFullTrunkFootprint) {
            Block base = context.world().getBlockAt(
                    logs.x(0),
                    baseY,
                    logs.z(0)
            );

            base.setType(
                    sapling,
                    false
            );

            return;
        }

        LongPositionSet replanted =
                new LongPositionSet();

        for (int index = 0; index < logs.size(); index++) {
            if (logs.y(index) != baseY) {
                continue;
            }

            int x = logs.x(index);
            int z = logs.z(index);

            if (!replanted.add(x, z)) {
                continue;
            }

            Block position = context.world().getBlockAt(
                    x,
                    baseY,
                    z
            );

            if (!position.getType().isAir()) {
                continue;
            }

            position.setType(
                    sapling,
                    false
            );
        }
    }

    private static Set<Material> parseMaterials(
            List<XMaterial> configured,
            String name
    ) {
        Set<Material> materials =
                parseOptionalMaterials(configured);

        if (materials.isEmpty()) {
            throw new IllegalArgumentException(
                    "Lumberjack " + name
                            + " must contain at least one valid material"
            );
        }

        return materials;
    }

    private static Set<Material> parseOptionalMaterials(
            List<XMaterial> configured
    ) {
        if (configured == null || configured.isEmpty()) {
            return Set.of();
        }

        EnumSet<Material> materials =
                EnumSet.noneOf(Material.class);

        for (XMaterial material : configured) {
            if (material == null) {
                continue;
            }

            Material parsed = material.parseMaterial();

            if (parsed != null) {
                materials.add(parsed);
            }
        }

        if (materials.isEmpty()) {
            return Set.of();
        }

        return Set.copyOf(materials);
    }

    private static Map<Material, Material> parseSaplingByLog(
            Map<XMaterial, XMaterial> configured
    ) {
        if (configured == null || configured.isEmpty()) {
            return Map.of();
        }

        Map<Material, Material> saplingByLog =
                new EnumMap<>(Material.class);

        for (Map.Entry<XMaterial, XMaterial> entry : configured.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }

            Material log = entry.getKey().parseMaterial();
            Material sapling = entry.getValue().parseMaterial();

            if (log != null && sapling != null) {
                saplingByLog.put(log, sapling);
            }
        }

        return Map.copyOf(saplingByLog);
    }

    private static Material requireMaterial(
            XMaterial configured,
            String name
    ) {
        Material material = configured == null
                ? null
                : configured.parseMaterial();

        if (material == null) {
            throw new IllegalArgumentException(
                    "Lumberjack " + name
                            + " material is unavailable: "
                            + configured
            );
        }

        return material;
    }

    private static MinionStatus resolveIdleStatus(
            boolean foundSapling,
            boolean foundInvalidStation
    ) {
        if (foundSapling) {
            return LumberjackStatuses.WAITING_FOR_TREE;
        }

        if (foundInvalidStation) {
            return LumberjackStatuses.INVALID_STATION;
        }

        return LumberjackStatuses.NO_SAPLING;
    }

    private static int expectedDropCapacity(
            int blockCount
    ) {
        long expectedCapacity =
                (long) blockCount * EXPECTED_DROPS_PER_BLOCK;

        return (int) Math.min(
                expectedCapacity,
                Integer.MAX_VALUE - 8L
        );
    }

    private static final class LongPositionSet {

        private final it.unimi.dsi.fastutil.longs.LongOpenHashSet positions =
                new it.unimi.dsi.fastutil.longs.LongOpenHashSet();

        private boolean add(int x, int z) {
            long position =
                    ((long) x << 32)
                            ^ (z & 0xFFFFFFFFL);

            return this.positions.add(position);
        }
    }
}
