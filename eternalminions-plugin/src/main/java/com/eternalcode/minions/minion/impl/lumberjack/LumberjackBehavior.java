package com.eternalcode.minions.minion.impl.lumberjack;

import com.eternalcode.minions.config.AbstractMinionConfig;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionBehavior;
import com.eternalcode.minions.minion.MinionContext;
import com.eternalcode.minions.minion.MinionDirection;
import com.eternalcode.minions.minion.MinionResult;
import com.eternalcode.minions.minion.status.CoreMinionStatuses;
import com.eternalcode.minions.minion.status.MinionStatus;
import com.eternalcode.minions.minion.tool.SpeedEnchant;
import com.eternalcode.minions.minion.tool.ToolCheck;
import com.eternalcode.minions.minion.tool.ToolDurabilityService;
import com.eternalcode.minions.minion.tool.ToolInventoryLocator;
import com.eternalcode.minions.minion.tool.ToolRequirement;
import com.eternalcode.minions.minion.tool.ToolValidationService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

public final class LumberjackBehavior implements MinionBehavior {

    private static final int EXPECTED_DROPS_PER_LOG = 2;

    private final LumberjackConfig config;
    private final ToolValidationService toolValidation;
    private final ToolDurabilityService toolDurability;
    private final ToolInventoryLocator toolLocator;
    private final ToolRequirement toolRequirement;
    private final TreeScanner treeScanner = new TreeScanner();
    private final Material logMaterial;
    private final Material saplingMaterial;

    public LumberjackBehavior(
        LumberjackConfig config,
        ToolValidationService toolValidation,
        ToolDurabilityService toolDurability,
        ToolInventoryLocator toolLocator
    ) {
        this.config = config;
        this.toolValidation = toolValidation;
        this.toolDurability = toolDurability;
        this.toolLocator = toolLocator;
        this.toolRequirement = config.toolRequirement();
        this.logMaterial = requireMaterial(config.logMaterial);
        this.saplingMaterial = requireMaterial(config.saplingMaterial);
        if (config.maxLogsPerTree < 1 || config.maxLogsPerTree > 4096) {
            throw new IllegalArgumentException("Lumberjack max logs per tree must be between 1 and 4096");
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
        Minion minion = context.retireWornTool(
            context.minion(),
            this.toolRequirement,
            this.toolValidation,
            this.toolLocator
        );
        ToolCheck toolCheck = this.toolValidation.validate(
            this.toolRequirement,
            minion.equipment().tool(),
            LumberjackStatuses.NO_AXE
        );
        if (toolCheck instanceof ToolCheck.Stopped stopped) {
            return MinionResult.idle(minion, stopped.reason());
        }
        if (!context.hasStorageRoom()) {
            return MinionResult.idle(minion, CoreMinionStatuses.STORAGE_FULL);
        }

        int stationCount = this.config.stationCount(minion.upgrades());
        MinionDirection direction = minion.settings().direction();
        int firstStationIndex = context.scheduledMinion().miningTargetIndex(stationCount);
        context.scheduledMinion().advanceMiningTarget(stationCount);
        boolean foundSapling = false;
        boolean foundInvalidStation = false;

        for (int offset = 0; offset < stationCount; offset++) {
            int distance = (firstStationIndex + offset) % stationCount + 1;
            int stationX = minion.position().blockX() + direction.offsetX() * distance;
            int stationZ = minion.position().blockZ() + direction.offsetZ() * distance;
            if (!context.world().isChunkLoaded(stationX >> 4, stationZ >> 4)) {
                continue;
            }

            Block station = context.world().getBlockAt(stationX, minion.position().blockY(), stationZ);
            Material stationMaterial = station.getType();
            if (stationMaterial == this.logMaterial) {
                context.scheduledMinion().face(direction.yaw());
                return this.fellTree(context, minion, station);
            }
            if (stationMaterial == this.saplingMaterial) {
                foundSapling = true;
                continue;
            }
            if (stationMaterial != Material.AIR) {
                foundInvalidStation = true;
            }
        }

        return MinionResult.idle(minion, resolveIdleStatus(foundSapling, foundInvalidStation));
    }

    private MinionResult fellTree(MinionContext context, Minion minion, Block trunkBase) {
        TreeScanner.ScanResult tree = this.treeScanner.scan(
            context.world(),
            trunkBase.getX(),
            trunkBase.getY(),
            trunkBase.getZ(),
            this.logMaterial,
            this.config.maxLogsPerTree
        );
        if (tree.isEmpty()) {
            return MinionResult.idle(minion, LumberjackStatuses.INVALID_STATION);
        }

        ItemStack tool = minion.equipment().tool();
        List<ItemStack> drops = new ArrayList<>(expectedDropCapacity(tree.size()));
        for (int treeIndex = 0; treeIndex < tree.size(); treeIndex++) {
            Block log = context.world().getBlockAt(tree.x(treeIndex), tree.y(treeIndex), tree.z(treeIndex));
            Collection<ItemStack> blockDrops = tool == null ? log.getDrops() : log.getDrops(tool);
            drops.addAll(blockDrops);
            log.setType(Material.AIR, false);
        }
        trunkBase.setType(this.saplingMaterial, false);

        Minion updated = this.consumeTool(minion, tool, tree.size());
        updated = context.deposit(updated, trunkBase.getLocation(), drops);
        updated = updated.withProgress(updated.progress().advanced(this.config));
        MinionResult result = MinionResult.worked(updated, LumberjackStatuses.CUTTING);
        if (!this.config.respectSpeedEnchants) {
            return result;
        }

        long baseInterval = this.config.workInterval(updated.upgrades());
        return result.withDelay(SpeedEnchant.scaledInterval(baseInterval, tool));
    }

    private Minion consumeTool(Minion minion, ItemStack tool, int uses) {
        if (tool == null || uses < 1) {
            return minion;
        }
        ItemStack damagedTool = this.toolDurability.consume(tool, uses);
        if (tool.isSimilar(damagedTool)) {
            return minion;
        }
        return minion.withEquipment(minion.equipment().withTool(damagedTool));
    }

    private static Material requireMaterial(com.cryptomorin.xseries.XMaterial configuredMaterial) {
        Material material = configuredMaterial.parseMaterial();
        if (material == null) {
            throw new IllegalArgumentException("Lumberjack material is unavailable: " + configuredMaterial);
        }
        return material;
    }

    private static MinionStatus resolveIdleStatus(boolean foundSapling, boolean foundInvalidStation) {
        if (foundSapling) {
            return LumberjackStatuses.WAITING_FOR_TREE;
        }
        if (foundInvalidStation) {
            return LumberjackStatuses.INVALID_STATION;
        }
        return LumberjackStatuses.NO_SAPLING;
    }

    private static int expectedDropCapacity(int logCount) {
        long expectedCapacity = (long) logCount * EXPECTED_DROPS_PER_LOG;
        return (int) Math.min(expectedCapacity, Integer.MAX_VALUE - 8L);
    }

}
