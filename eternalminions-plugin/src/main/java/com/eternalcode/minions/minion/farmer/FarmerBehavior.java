package com.eternalcode.minions.minion.farmer;

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
import com.eternalcode.minions.minion.tool.ToolCheck;
import com.eternalcode.minions.minion.tool.ToolDurabilityService;
import com.eternalcode.minions.minion.tool.ToolInventoryLocator;
import com.eternalcode.minions.minion.tool.ToolValidationService;
import com.eternalcode.minions.render.MinionRenderer;
import java.util.Collection;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.inventory.ItemStack;

// Harvests only mature crops at fixed stations in front of the minion, then replants: ordinary
// crops need a seed consumed from the drop, sugar cane/bamboo only lose their topmost extra
// segment, and pumpkin/melon fruit blocks regrow on their own from an untouched stem.
public final class FarmerBehavior extends AbstractMinionBehavior {

    private final ToolValidationService toolValidation;
    private final ToolDurabilityService toolDurability;
    private final ToolInventoryLocator toolLocator;

    public FarmerBehavior(
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
    }

    @Override
    public boolean execute(Minion minion, MinionType type, ScheduledMinion scheduledMinion, World world) {
        minion = this.retireWornToolIfNeeded(minion, type, world, this.toolValidation, this.toolLocator);

        ToolCheck toolCheck = this.toolValidation.validate(
            type.work().toolRequirement(), minion.equipment().tool(), FarmerStatuses.NO_HOE);
        if (toolCheck instanceof ToolCheck.Stopped stopped) {
            this.refreshStatusIfChanged(minion, stopped.reason());
            return false;
        }

        if (!this.hasStorageRoom(minion, world)) {
            this.refreshStatusIfChanged(minion, CoreMinionStatuses.STORAGE_FULL);
            return false;
        }

        FarmerWork work = type.work().farmer();
        int stations = Math.max(1, type.miningRadius(minion.upgrades()));
        MinionDirection direction = minion.settings().direction();

        for (int checked = 0; checked < stations; checked++) {
            int stationIndex = scheduledMinion.miningTargetIndex(stations);
            scheduledMinion.advanceMiningTarget(stations);
            int distance = stationIndex + 1;
            int stationX = minion.position().blockX() + direction.offsetX() * distance;
            int stationZ = minion.position().blockZ() + direction.offsetZ() * distance;
            int stationY = minion.position().blockY();
            if (!world.isChunkLoaded(stationX >> 4, stationZ >> 4)) {
                continue;
            }

            Block station = world.getBlockAt(stationX, stationY, stationZ);
            if (!work.cropMaterials().contains(station.getType())) {
                continue;
            }

            Boolean outcome = this.tryHarvest(minion, type, world, work, station);
            if (outcome != null) {
                return outcome;
            }
        }

        this.refreshStatusIfChanged(minion, FarmerStatuses.NO_MATURE_CROPS);
        return false;
    }

    // Returns null when this station was not (yet) harvestable, so the caller keeps scanning;
    // otherwise returns this action's actual execute() outcome.
    private Boolean tryHarvest(Minion minion, MinionType type, World world, FarmerWork work, Block station) {
        return switch (CropShape.of(station.getType())) {
            case ADJACENT_STEM_FRUIT -> this.harvestFruit(minion, type, world, station);
            case STACKING_COLUMN -> this.harvestColumn(minion, type, world, station);
            case AGEABLE_REPLANT -> this.harvestAgeable(minion, type, world, work, station);
        };
    }

    // Pumpkin/melon fruit blocks are always harvestable the instant they exist - the stem grows
    // them on its own and is never itself a station, so there is no maturity check here.
    private Boolean harvestFruit(Minion minion, MinionType type, World world, Block station) {
        ItemStack tool = minion.equipment().tool();
        Collection<ItemStack> drops = tool == null ? station.getDrops() : station.getDrops(tool);
        station.setType(Material.AIR, false);
        return this.finishHarvest(minion, type, world, station, drops, tool, FarmerStatuses.HARVESTING);
    }

    private Boolean harvestColumn(Minion minion, MinionType type, World world, Block station) {
        Block above = station.getRelative(BlockFace.UP);
        if (above.getType() != station.getType()) {
            // Only one segment tall - trimming it would destroy the base block itself.
            return null;
        }
        ItemStack tool = minion.equipment().tool();
        Collection<ItemStack> drops = tool == null ? above.getDrops() : above.getDrops(tool);
        above.setType(Material.AIR, false);
        return this.finishHarvest(minion, type, world, station, drops, tool, FarmerStatuses.HARVESTING);
    }

    private Boolean harvestAgeable(Minion minion, MinionType type, World world, FarmerWork work, Block station) {
        if (!(station.getBlockData() instanceof Ageable ageable) || ageable.getAge() < ageable.getMaximumAge()) {
            return null;
        }

        ItemStack tool = minion.equipment().tool();
        Collection<ItemStack> drops = tool == null ? station.getDrops() : station.getDrops(tool);
        Material seedMaterial = work.seedByCrop().get(station.getType());
        ItemStack seedToConsume = seedMaterial == null ? null : findSeed(drops, seedMaterial);

        if (seedMaterial != null && seedToConsume == null) {
            // Harvested anyway (the drop is never withheld) but left unplanted - see status text.
            station.setType(Material.AIR, false);
            return this.finishHarvest(minion, type, world, station, drops, tool, FarmerStatuses.NO_SEEDS);
        }

        if (seedToConsume != null) {
            seedToConsume.setAmount(seedToConsume.getAmount() - 1);
        }
        ageable.setAge(0);
        station.setBlockData(ageable, false);
        return this.finishHarvest(minion, type, world, station, drops, tool, FarmerStatuses.HARVESTING);
    }

    private static ItemStack findSeed(Collection<ItemStack> drops, Material seedMaterial) {
        for (ItemStack drop : drops) {
            if (drop.getType() == seedMaterial) {
                return drop;
            }
        }
        return null;
    }

    private boolean finishHarvest(
        Minion minion, MinionType type, World world,
        Block station, Collection<ItemStack> drops, ItemStack tool, MinionStatus status
    ) {
        Minion updated = this.consumeTool(minion, tool);
        this.deposit(updated, type, world, station.getLocation(), drops, Float.NaN);
        this.refreshStatusIfChanged(updated, status);
        return true;
    }

    private Minion consumeTool(Minion minion, ItemStack tool) {
        if (tool == null) {
            return minion;
        }

        ItemStack damagedTool = this.toolDurability.consume(tool, 1);
        Minion updated = minion.withEquipment(minion.equipment().withTool(damagedTool));
        this.registry.replace(updated);
        this.persistence.saveEquipment(updated);
        this.renderer.refreshEquipment(updated.id(), damagedTool);
        return updated;
    }
}
