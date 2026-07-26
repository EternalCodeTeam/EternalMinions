package com.eternalcode.minions.minion.impl.farmer;

import com.cryptomorin.xseries.XMaterial;
import com.eternalcode.minions.config.AbstractMinionConfig;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionBehavior;
import com.eternalcode.minions.minion.MinionContext;
import com.eternalcode.minions.minion.MinionDirection;
import com.eternalcode.minions.minion.MinionResult;
import com.eternalcode.minions.minion.status.CoreMinionStatuses;
import com.eternalcode.minions.minion.status.MinionStatus;
import com.eternalcode.minions.minion.tool.ToolCheck;
import com.eternalcode.minions.minion.tool.ToolDurabilityService;
import com.eternalcode.minions.minion.tool.ToolInventoryLocator;
import com.eternalcode.minions.minion.tool.ToolRequirement;
import com.eternalcode.minions.minion.tool.ToolValidationService;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.inventory.ItemStack;

public final class FarmerBehavior implements MinionBehavior {

    private final FarmerConfig config;
    private final ToolValidationService toolValidation;
    private final ToolDurabilityService toolDurability;
    private final ToolInventoryLocator toolLocator;
    private final ToolRequirement toolRequirement;
    private final Set<Material> cropMaterials;
    private final Map<Material, Material> seedByCrop;

    public FarmerBehavior(
        FarmerConfig config,
        ToolValidationService toolValidation,
        ToolDurabilityService toolDurability,
        ToolInventoryLocator toolLocator
    ) {
        this.config = config;
        this.toolValidation = toolValidation;
        this.toolDurability = toolDurability;
        this.toolLocator = toolLocator;
        this.toolRequirement = config.toolRequirement();
        Set<Material> configuredCrops = config.materials(config.cropMaterials);
        this.cropMaterials = configuredCrops.isEmpty() ? Set.of(Material.WHEAT) : configuredCrops;
        this.seedByCrop = mapSeeds(config.seedByCrop);
    }

    @Override
    public String id() {
        return "farmer";
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
            FarmerStatuses.NO_HOE
        );
        if (toolCheck instanceof ToolCheck.Stopped stopped) {
            return MinionResult.idle(minion, stopped.reason());
        }
        if (!context.hasStorageRoom()) {
            return MinionResult.idle(minion, CoreMinionStatuses.STORAGE_FULL);
        }

        int stationCount = this.config.stationCount(minion.upgrades());
        MinionDirection direction = minion.settings().direction();
        for (int checkedStations = 0; checkedStations < stationCount; checkedStations++) {
            int stationIndex = context.scheduledMinion().miningTargetIndex(stationCount);
            context.scheduledMinion().advanceMiningTarget(stationCount);
            int distance = stationIndex + 1;
            int stationX = minion.position().blockX() + direction.offsetX() * distance;
            int stationZ = minion.position().blockZ() + direction.offsetZ() * distance;
            int stationY = minion.position().blockY();
            if (!context.world().isChunkLoaded(stationX >> 4, stationZ >> 4)) {
                continue;
            }

            Block station = context.world().getBlockAt(stationX, stationY, stationZ);
            if (!this.cropMaterials.contains(station.getType())) {
                continue;
            }

            MinionResult result = this.tryHarvest(context, minion, station);
            if (result != null) {
                context.scheduledMinion().face(direction.yaw());
                return result;
            }
        }
        return MinionResult.idle(minion, FarmerStatuses.NO_MATURE_CROPS);
    }

    private MinionResult tryHarvest(MinionContext context, Minion minion, Block station) {
        return switch (CropShape.of(station.getType())) {
            case ADJACENT_STEM_FRUIT -> this.harvestFruit(context, minion, station);
            case STACKING_COLUMN -> this.harvestColumn(context, minion, station);
            case AGEABLE_REPLANT -> this.harvestAgeable(context, minion, station);
        };
    }

    private MinionResult harvestFruit(MinionContext context, Minion minion, Block station) {
        ItemStack tool = minion.equipment().tool();
        Collection<ItemStack> drops = tool == null ? station.getDrops() : station.getDrops(tool);
        station.setType(Material.AIR, false);
        return this.finishHarvest(context, minion, station, drops, tool, FarmerStatuses.HARVESTING);
    }

    private MinionResult harvestColumn(MinionContext context, Minion minion, Block station) {
        Block above = station.getRelative(BlockFace.UP);
        if (above.getType() != station.getType()) {
            return null;
        }

        ItemStack tool = minion.equipment().tool();
        Collection<ItemStack> drops = tool == null ? above.getDrops() : above.getDrops(tool);
        above.setType(Material.AIR, false);
        return this.finishHarvest(context, minion, station, drops, tool, FarmerStatuses.HARVESTING);
    }

    private MinionResult harvestAgeable(MinionContext context, Minion minion, Block station) {
        if (!(station.getBlockData() instanceof Ageable ageable) || ageable.getAge() < ageable.getMaximumAge()) {
            return null;
        }

        ItemStack tool = minion.equipment().tool();
        Collection<ItemStack> drops = tool == null ? station.getDrops() : station.getDrops(tool);
        Material seedMaterial = this.seedByCrop.get(station.getType());
        ItemStack seed = seedMaterial == null ? null : findSeed(drops, seedMaterial);
        if (seedMaterial != null && seed == null) {
            station.setType(Material.AIR, false);
            return this.finishHarvest(context, minion, station, drops, tool, FarmerStatuses.NO_SEEDS);
        }

        if (seed != null) {
            seed.setAmount(seed.getAmount() - 1);
        }
        ageable.setAge(0);
        station.setBlockData(ageable, false);
        return this.finishHarvest(context, minion, station, drops, tool, FarmerStatuses.HARVESTING);
    }

    private MinionResult finishHarvest(
        MinionContext context,
        Minion minion,
        Block station,
        Collection<ItemStack> drops,
        ItemStack tool,
        MinionStatus status
    ) {
        Minion updated = this.consumeTool(minion, tool);
        updated = context.deposit(updated, station.getLocation(), drops);
        updated = updated.withProgress(updated.progress().advanced(this.config));
        return MinionResult.worked(updated, status);
    }

    private Minion consumeTool(Minion minion, ItemStack tool) {
        if (tool == null) {
            return minion;
        }
        ItemStack damagedTool = this.toolDurability.consume(tool, 1);
        return minion.withEquipment(minion.equipment().withTool(damagedTool));
    }

    private static ItemStack findSeed(Collection<ItemStack> drops, Material seedMaterial) {
        for (ItemStack drop : drops) {
            if (drop.getType() == seedMaterial) {
                return drop;
            }
        }
        return null;
    }

    private static Map<Material, Material> mapSeeds(Map<XMaterial, XMaterial> configuredSeeds) {
        Map<Material, Material> seeds = new EnumMap<>(Material.class);
        for (Map.Entry<XMaterial, XMaterial> entry : configuredSeeds.entrySet()) {
            Material crop = entry.getKey().parseMaterial();
            Material seed = entry.getValue().parseMaterial();
            if (crop != null && seed != null) {
                seeds.put(crop, seed);
            }
        }
        return Map.copyOf(seeds);
    }

}
