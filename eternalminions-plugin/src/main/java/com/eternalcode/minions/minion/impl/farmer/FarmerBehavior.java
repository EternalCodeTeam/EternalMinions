package com.eternalcode.minions.minion.impl.farmer;

import com.cryptomorin.xseries.XMaterial;
import com.eternalcode.minions.config.AbstractMinionConfig;
import com.eternalcode.minions.config.ConfigService;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionBehavior;
import com.eternalcode.minions.minion.MinionBlockDrops;
import com.eternalcode.minions.minion.MinionContext;
import com.eternalcode.minions.minion.MinionDirection;
import com.eternalcode.minions.minion.MinionRotation;
import com.eternalcode.minions.minion.MinionResult;
import com.eternalcode.minions.minion.WorkLimit;
import com.eternalcode.minions.minion.storage.MinionItemTransferService;
import com.eternalcode.minions.minion.status.CoreMinionStatuses;
import com.eternalcode.minions.minion.status.MinionStatus;
import com.eternalcode.minions.minion.tool.MinionToolPreparation;
import com.eternalcode.minions.minion.tool.MinionToolService;
import com.eternalcode.minions.minion.tool.ToolCheck;
import com.eternalcode.minions.minion.tool.ToolRequirement;
import java.io.File;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.inventory.ItemStack;

public final class FarmerBehavior implements MinionBehavior {

    private final FarmerConfig config;

    private final MinionToolService tools;
    private final MinionItemTransferService transfers;
    private final ToolRequirement toolRequirement;

    private final Map<Material, CropShape> crops;
    private final Map<Material, Material> seeds;

    public FarmerBehavior(
            FarmerConfig config,
            MinionToolService tools,
            MinionItemTransferService transfers
    ) {
        this.config = config;

        this.tools = tools;
        this.transfers = transfers;
        this.toolRequirement = config.toolRequirement();

        this.crops = parseCrops(config.crops);
        this.seeds = parseMaterials(config.seeds);
        WorkLimit.validate("farmer.maxCropsPerCycle", config.maxCropsPerCycle);
    }

    public static FarmerBehavior create(
            ConfigService configs,
            File directory,
            MinionToolService tools,
            MinionItemTransferService transfers
    ) {
        FarmerConfig config = configs.load(
                FarmerConfig.class,
                new File(directory, "farmer.yml")
        );

        return new FarmerBehavior(
                config,
                tools,
                transfers
        );
    }

    private static boolean consumeOne(
            List<ItemStack> drops,
            Material material
    ) {
        Iterator<ItemStack> iterator = drops.iterator();

        while (iterator.hasNext()) {
            ItemStack drop = iterator.next();

            if (drop.getType() != material) {
                continue;
            }

            if (drop.getAmount() <= 1) {
                iterator.remove();
            }
            else {
                drop.setAmount(drop.getAmount() - 1);
            }

            return true;
        }

        return false;
    }

    private static Map<Material, CropShape> parseCrops(
            Map<XMaterial, CropShape> configuredCrops
    ) {
        Map<Material, CropShape> crops =
                new EnumMap<>(Material.class);

        for (
                Map.Entry<XMaterial, CropShape> entry
                : configuredCrops.entrySet()
        ) {
            Material material = entry.getKey().parseMaterial();
            CropShape shape = entry.getValue();

            if (material == null || shape == null) {
                continue;
            }

            crops.put(material, shape);
        }

        return Map.copyOf(crops);
    }

    private static Map<Material, Material> parseMaterials(
            Map<XMaterial, XMaterial> configuredMaterials
    ) {
        Map<Material, Material> materials =
                new EnumMap<>(Material.class);

        for (
                Map.Entry<XMaterial, XMaterial> entry
                : configuredMaterials.entrySet()
        ) {
            Material key = entry.getKey().parseMaterial();
            Material value = entry.getValue().parseMaterial();

            if (key == null || value == null) {
                continue;
            }

            materials.put(key, value);
        }

        return Map.copyOf(materials);
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
        MinionToolPreparation preparation = this.tools.prepare(
                context,
                this.toolRequirement,
                FarmerStatuses.NO_HOE
        );
        if (preparation.check() instanceof ToolCheck.Stopped(MinionStatus reason)) {
            return MinionResult.idle(preparation.minion(), reason);
        }
        Minion minion = preparation.minion();

        if (this.crops.isEmpty()) {
            return MinionResult.idle(
                    minion,
                    FarmerStatuses.NO_CONFIGURED_CROPS
            );
        }

        int range = this.config.range(minion.upgrades());
        int targetCount = this.targetCount(range);
        int workLimit = WorkLimit.resolve(this.config.maxCropsPerCycle, targetCount);
        int harvestedCrops = 0;
        Minion updated = minion;

        for (int checked = 0; checked < targetCount; checked++) {
            int targetIndex = context.scheduledMinion()
                    .miningTargetIndex(targetCount);

            context.scheduledMinion()
                    .advanceMiningTarget(targetCount);

            Target target = this.target(
                    updated,
                    range,
                    targetIndex
            );

            int blockX =
                    updated.position().blockX() + target.offsetX();

            int blockY =
                    updated.position().blockY() + this.config.cropYOffset;

            int blockZ =
                    updated.position().blockZ() + target.offsetZ();

            if (!context.world().isChunkLoaded(blockX >> 4, blockZ >> 4)) {
                continue;
            }

            Block crop = context.world().getBlockAt(
                    blockX,
                    blockY,
                    blockZ
            );

            CropShape shape = this.crops.get(crop.getType());

            if (shape == null) {
                continue;
            }

            MinionResult result = this.tryHarvest(
                    context,
                    updated,
                    crop,
                    shape
            );

            if (result == null) {
                continue;
            }
            if (!result.worked()) {
                return result;
            }

            context.scheduledMinion().face(
                    MinionRotation.yawTowards(0, 0, target.offsetX(), target.offsetZ())
            );
            updated = result.minion();
            harvestedCrops++;
            if (harvestedCrops >= workLimit) {
                break;
            }
        }

        if (harvestedCrops > 0) {
            return MinionResult.worked(updated, FarmerStatuses.HARVESTING);
        }
        return MinionResult.idle(
                updated,
                FarmerStatuses.NO_MATURE_CROPS
        );
    }

    private MinionResult tryHarvest(
            MinionContext context,
            Minion minion,
            Block crop,
            CropShape shape
    ) {
        return switch (shape) {
            case AGEABLE_REPLANT -> this.harvestAgeable(context, minion, crop);

            case STACKING_COLUMN -> this.harvestColumn(context, minion, crop);

            case STEM_FRUIT -> this.harvestFruit(context, minion, crop);
        };
    }

    private MinionResult harvestAgeable(
            MinionContext context,
            Minion minion,
            Block crop
    ) {
        if (!(crop.getBlockData() instanceof Ageable ageable)) {
            return null;
        }

        if (ageable.getAge() < ageable.getMaximumAge()) {
            return null;
        }

        ItemStack tool = minion.equipment().tool();
        List<ItemStack> drops = MinionBlockDrops.collect(crop, tool);

        Material seed = this.seeds.get(crop.getType());

        if (seed != null && !consumeOne(drops, seed)) {
            return MinionResult.idle(
                    minion,
                    FarmerStatuses.NO_SEEDS
            );
        }
        if (!this.transfers.canStoreAll(context, minion.storage(), drops)) {
            return MinionResult.idle(minion, CoreMinionStatuses.STORAGE_FULL);
        }

        ageable.setAge(0);
        crop.setBlockData(ageable, false);

        return this.finishHarvest(
                context,
                minion,
                crop,
                drops,
                FarmerStatuses.HARVESTING
        );
    }

    private MinionResult harvestColumn(
            MinionContext context,
            Minion minion,
            Block crop
    ) {
        Block harvested = this.findTopColumnBlock(crop);

        if (harvested == null) {
            return null;
        }

        ItemStack tool = minion.equipment().tool();
        List<ItemStack> drops = MinionBlockDrops.collect(harvested, tool);
        if (!this.transfers.canStoreAll(context, minion.storage(), drops)) {
            return MinionResult.idle(minion, CoreMinionStatuses.STORAGE_FULL);
        }

        harvested.setType(Material.AIR, false);

        return this.finishHarvest(
                context,
                minion,
                harvested,
                drops,
                FarmerStatuses.HARVESTING
        );
    }

    private MinionResult harvestFruit(
            MinionContext context,
            Minion minion,
            Block fruit
    ) {
        ItemStack tool = minion.equipment().tool();
        List<ItemStack> drops = MinionBlockDrops.collect(fruit, tool);
        if (!this.transfers.canStoreAll(context, minion.storage(), drops)) {
            return MinionResult.idle(minion, CoreMinionStatuses.STORAGE_FULL);
        }

        fruit.setType(Material.AIR, false);

        return this.finishHarvest(
                context,
                minion,
                fruit,
                drops,
                FarmerStatuses.HARVESTING
        );
    }

    private MinionResult finishHarvest(
            MinionContext context,
            Minion minion,
            Block harvested,
            Collection<ItemStack> drops,
            MinionStatus status
    ) {
        Minion updated = this.tools.consume(minion, 1);

        updated = this.transfers.deposit(
                context,
                updated,
                harvested.getLocation(),
                drops
        );

        updated = updated.withProgress(
                updated.progress().advanced(this.config)
        );

        return MinionResult.worked(updated, status);
    }

    private Block findTopColumnBlock(Block base) {
        Material material = base.getType();
        Block above = base.getRelative(BlockFace.UP);

        if (above.getType() != material) {
            return null;
        }

        Block highest = above;
        int maximumY = highest.getWorld().getMaxHeight() - 1;

        while (highest.getY() < maximumY) {
            Block next = highest.getRelative(BlockFace.UP);

            if (next.getType() != material) {
                break;
            }

            highest = next;
        }

        return highest;
    }

    private int targetCount(int range) {
        if (this.config.workMode == FarmerConfig.WorkMode.LINE) {
            return range;
        }

        int sideLength = range * 2 + 1;

        return sideLength * sideLength - 1;
    }

    private Target target(
            Minion minion,
            int range,
            int targetIndex
    ) {
        MinionDirection direction = minion.settings().direction();

        if (this.config.workMode == FarmerConfig.WorkMode.LINE) {
            int distance = targetIndex + 1;

            return new Target(
                    direction.offsetX() * distance,
                    direction.offsetZ() * distance
            );
        }

        int sideLength = range * 2 + 1;
        int centerIndex = range * sideLength + range;

        int rawIndex = targetIndex >= centerIndex
                ? targetIndex + 1
                : targetIndex;

        int offsetX = rawIndex % sideLength - range;
        int offsetZ = rawIndex / sideLength - range;

        return new Target(offsetX, offsetZ);
    }

    private record Target(
            int offsetX,
            int offsetZ
    ) {
    }
}
