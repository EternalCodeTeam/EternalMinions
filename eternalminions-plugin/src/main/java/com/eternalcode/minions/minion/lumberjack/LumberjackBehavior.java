package com.eternalcode.minions.minion.lumberjack;

import com.eternalcode.minions.database.MinionPersistenceService;
import com.eternalcode.minions.minion.AbstractMinionBehavior;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionDirection;
import com.eternalcode.minions.minion.MinionRegistry;
import com.eternalcode.minions.minion.MinionType;
import com.eternalcode.minions.minion.ScheduledMinion;
import com.eternalcode.minions.minion.status.CoreMinionStatuses;
import com.eternalcode.minions.minion.status.MinionStatus;
import com.eternalcode.minions.minion.status.MinionStatusTracker;
import com.eternalcode.minions.minion.tool.SpeedEnchant;
import com.eternalcode.minions.minion.tool.ToolCheck;
import com.eternalcode.minions.minion.tool.ToolDurabilityService;
import com.eternalcode.minions.minion.tool.ToolInventoryLocator;
import com.eternalcode.minions.minion.tool.ToolValidationService;
import com.eternalcode.minions.render.MinionRenderer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

public final class LumberjackBehavior extends AbstractMinionBehavior {

    private static final int EXPECTED_DROPS_PER_LOG = 2;

    private final ToolValidationService toolValidation;
    private final ToolDurabilityService toolDurability;
    private final ToolInventoryLocator toolLocator;
    private final TreeScanner treeScanner;

    public LumberjackBehavior(
            MinionRegistry registry,
            MinionPersistenceService persistence,
            MinionRenderer renderer,
            MinionStatusTracker statuses,
            ToolValidationService toolValidation,
            ToolDurabilityService toolDurability,
            ToolInventoryLocator toolLocator
    ) {
        super(registry, persistence, renderer, statuses);

        this.toolValidation = toolValidation;
        this.toolDurability = toolDurability;
        this.toolLocator = toolLocator;
        this.treeScanner = new TreeScanner();
    }

    @Override
    public boolean execute(
            Minion minion,
            MinionType type,
            ScheduledMinion scheduledMinion,
            World world
    ) {
        Minion preparedMinion = this.retireWornToolIfNeeded(
                minion,
                type,
                world,
                this.toolValidation,
                this.toolLocator
        );

        ToolCheck toolCheck = this.toolValidation.validate(
                type.work().toolRequirement(),
                preparedMinion.equipment().tool(),
                LumberjackStatuses.NO_AXE
        );

        if (toolCheck instanceof ToolCheck.Stopped stopped) {
            this.refreshStatusIfChanged(preparedMinion, stopped.reason());
            return false;
        }

        if (!this.hasStorageRoom(preparedMinion, world)) {
            this.refreshStatusIfChanged(
                    preparedMinion,
                    CoreMinionStatuses.STORAGE_FULL
            );

            return false;
        }

        LumberjackWork work = type.work().lumberjack();
        int stationCount = Math.max(
                1,
                type.miningRadius(preparedMinion.upgrades())
        );

        MinionDirection direction = preparedMinion.settings().direction();
        int firstStationIndex = scheduledMinion.miningTargetIndex(stationCount);

        scheduledMinion.advanceMiningTarget(stationCount);

        boolean foundSapling = false;
        boolean foundInvalidStation = false;

        int baseX = preparedMinion.position().blockX();
        int baseY = preparedMinion.position().blockY();
        int baseZ = preparedMinion.position().blockZ();

        for (int offset = 0; offset < stationCount; offset++) {
            int stationIndex = (firstStationIndex + offset) % stationCount;
            int distance = stationIndex + 1;

            int stationX = baseX + direction.offsetX() * distance;
            int stationZ = baseZ + direction.offsetZ() * distance;

            if (!world.isChunkLoaded(stationX >> 4, stationZ >> 4)) {
                continue;
            }

            Block station = world.getBlockAt(
                    stationX,
                    baseY,
                    stationZ
            );

            Material stationMaterial = station.getType();

            if (stationMaterial == work.logMaterial()) {
                return this.fellTree(
                        preparedMinion,
                        type,
                        scheduledMinion,
                        world,
                        work,
                        station
                );
            }

            if (stationMaterial == work.saplingMaterial()) {
                foundSapling = true;
                continue;
            }

            if (stationMaterial != Material.AIR) {
                foundInvalidStation = true;
            }
        }

        MinionStatus status = resolveIdleStatus(
                foundSapling,
                foundInvalidStation
        );

        this.refreshStatusIfChanged(preparedMinion, status);
        return false;
    }

    private boolean fellTree(
            Minion minion,
            MinionType type,
            ScheduledMinion scheduledMinion,
            World world,
            LumberjackWork work,
            Block trunkBase
    ) {
        TreeScanner.ScanResult tree = this.treeScanner.scan(
                world,
                trunkBase.getX(),
                trunkBase.getY(),
                trunkBase.getZ(),
                work.logMaterial(),
                work.maxLogsPerTree()
        );

        if (tree.isEmpty()) {
            this.refreshStatusIfChanged(
                    minion,
                    LumberjackStatuses.INVALID_STATION
            );

            return false;
        }

        ItemStack tool = minion.equipment().tool();
        List<ItemStack> drops = new ArrayList<>(
                expectedDropCapacity(tree.size())
        );

        for (int index = 0; index < tree.size(); index++) {
            Block log = world.getBlockAt(
                    tree.x(index),
                    tree.y(index),
                    tree.z(index)
            );

            Collection<ItemStack> blockDrops = tool == null
                    ? log.getDrops()
                    : log.getDrops(tool);

            drops.addAll(blockDrops);
            log.setType(Material.AIR, false);
        }

        trunkBase.setType(
                work.saplingMaterial(),
                false
        );

        Minion updatedMinion = this.consumeTool(
                minion,
                tool,
                tree.size()
        );

        this.deposit(
                updatedMinion,
                type,
                world,
                trunkBase.getLocation(),
                drops,
                Float.NaN
        );

        this.refreshStatusIfChanged(
                updatedMinion,
                LumberjackStatuses.CUTTING
        );

        if (type.respectSpeedEnchants()) {
            long baseInterval = type.workIntervalTicks(updatedMinion.upgrades());
            scheduledMinion.forceNextDelay(SpeedEnchant.scaledInterval(baseInterval, tool));
        }

        return true;
    }

    private Minion consumeTool(
            Minion minion,
            ItemStack tool,
            int uses
    ) {
        if (tool == null || uses <= 0) {
            return minion;
        }

        ItemStack damagedTool = this.toolDurability.consume(
                tool,
                uses
        );

        if (tool.isSimilar(damagedTool)) {
            return minion;
        }

        Minion updatedMinion = minion.withEquipment(
                minion.equipment().withTool(damagedTool)
        );

        this.registry.replace(updatedMinion);
        this.persistence.saveEquipment(updatedMinion);
        this.renderer.refreshEquipment(
                updatedMinion.id(),
                damagedTool
        );

        return updatedMinion;
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

    private static int expectedDropCapacity(int logCount) {
        long expectedCapacity = (long) logCount * EXPECTED_DROPS_PER_LOG;

        return (int) Math.min(
                expectedCapacity,
                Integer.MAX_VALUE - 8L
        );
    }
}