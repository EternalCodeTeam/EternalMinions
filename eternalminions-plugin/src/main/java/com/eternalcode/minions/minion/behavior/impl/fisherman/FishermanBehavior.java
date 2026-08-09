package com.eternalcode.minions.minion.behavior.impl.fisherman;

import com.cryptomorin.xseries.XMaterial;
import com.eternalcode.minions.config.AbstractMinionConfig;
import com.eternalcode.minions.config.ConfigService;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.behavior.MinionBehavior;
import com.eternalcode.minions.minion.MinionContext;
import com.eternalcode.minions.minion.rotation.MinionRotation;
import com.eternalcode.minions.minion.MinionResult;
import com.eternalcode.minions.minion.storage.MinionItemTransferService;
import com.eternalcode.minions.minion.status.CoreMinionStatuses;
import com.eternalcode.minions.minion.status.MinionStatus;
import com.eternalcode.minions.minion.tool.EnchantmentLevels;
import com.eternalcode.minions.minion.tool.MinionToolPreparation;
import com.eternalcode.minions.minion.tool.MinionToolService;
import com.eternalcode.minions.minion.tool.ToolCheck;
import com.eternalcode.minions.minion.tool.ToolRequirement;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

public final class FishermanBehavior implements MinionBehavior {

    private final FishermanConfig config;

    private final MinionToolService tools;
    private final MinionItemTransferService transfers;
    private final ToolRequirement toolRequirement;

    private final WaterBodyScanner scanner =
            new WaterBodyScanner();

    public static FishermanBehavior create(
            ConfigService configs,
            MinionToolService tools,
            MinionItemTransferService transfers
    ) {
        FishermanConfig config = configs.get(FishermanConfig.class);

        return new FishermanBehavior(
                config,
                tools,
                transfers
        );
    }

    public FishermanBehavior(
            FishermanConfig config,
            MinionToolService tools,
            MinionItemTransferService transfers
    ) {
        this.config = config;

        this.tools = tools;
        this.transfers = transfers;
        this.toolRequirement = config.toolRequirement();
        if (config.catchesPerCycle < 1) {
            throw new IllegalArgumentException(
                    "fisherman.catchesPerCycle must be positive: " + config.catchesPerCycle
            );
        }
    }

    @Override
    public String id() {
        return "fisherman";
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
                FishermanStatuses.NO_ROD
        );
        if (preparation.check() instanceof ToolCheck.Stopped stopped) {
            context.scheduledMinion().clearBusyTimer();

            return MinionResult.idle(
                    preparation.minion(),
                    stopped.reason()
            );
        }
        Minion minion = preparation.minion();

        WaterBodyScanner.ScanResult waterResult =
                this.findFishingWater(context);

        if (!waterResult.valid()) {
            context.scheduledMinion().clearBusyTimer();

            return MinionResult.idle(
                    minion,
                    status(waterResult.failure())
            );
        }

        Block water = waterResult.water();

        context.scheduledMinion().face(
                MinionRotation.yawTowards(
                        minion.position().blockX(),
                        minion.position().blockZ(),
                        water.getX(),
                        water.getZ()
                )
        );

        Minion updated = minion;
        MinionResult catchResult = null;
        for (int catchNumber = 0; catchNumber < this.config.catchesPerCycle; catchNumber++) {
            ItemStack rod = updated.equipment().tool();
            if (rod == null || rod.getType().isAir()) {
                break;
            }
            catchResult = this.catchFish(context, updated, rod, water);
            if (!catchResult.worked()) {
                context.scheduledMinion().clearBusyTimer();
                return catchResult;
            }
            updated = catchResult.minion();
        }
        if (catchResult == null) {
            return MinionResult.idle(updated, FishermanStatuses.NO_ROD);
        }
        return new MinionResult(
                updated,
                catchResult.status(),
                true,
                catchResult.delayTicks()
        );
    }

    private MinionResult catchFish(
            MinionContext context,
            Minion minion,
            ItemStack rod,
            Block water
    ) {
        Optional<ItemStack> caughtItem = this.randomCatch(rod);
        if (caughtItem.isPresent()
                && !this.transfers.canStoreAll(context, minion.storage(), List.of(caughtItem.get()))) {
            return MinionResult.idle(minion, CoreMinionStatuses.STORAGE_FULL);
        }

        Minion updated = this.tools.consume(minion, 1);

        if (caughtItem.isPresent()) {
            updated = this.transfers.deposit(
                    context,
                    updated,
                    water.getLocation(),
                    List.of(caughtItem.get())
            );
        }

        updated = updated.withProgress(
                updated.progress().advanced(this.config)
        );

        int lureLevel = EnchantmentLevels.level(rod, Enchantment.LURE);
        MinionStatus status = caughtItem.isPresent()
                ? FishermanStatuses.CATCHING
                : FishermanStatuses.NOTHING_CAUGHT;

        return MinionResult
                .worked(updated, status)
                .withDelay(this.config.fishingWaitTicks(
                        lureLevel,
                        updated.upgrades()
                ));
    }

    private Optional<ItemStack> randomCatch(ItemStack rod) {
        double emptyCatchChance = Math.clamp(
                this.config.emptyCatchChance,
                0.0,
                100.0
        );

        if (ThreadLocalRandom.current().nextDouble(100.0) < emptyCatchChance) {
            return Optional.empty();
        }

        int luckLevel = EnchantmentLevels.level(
                rod,
                Enchantment.LUCK_OF_THE_SEA
        );

        double fishWeight = Math.max(0.0, this.config.fishCategoryWeight);
        double junkWeight = Math.max(
                0.0,
                this.config.junkCategoryWeight
                        - (luckLevel * this.config.luckJunkWeightReductionPerLevel)
        );
        double treasureWeight = Math.max(
                0.0,
                this.config.treasureCategoryWeight
                        + (luckLevel * this.config.luckTreasureWeightPerLevel)
        );
        double totalWeight = fishWeight + junkWeight + treasureWeight;

        if (totalWeight <= 0.0) {
            return Optional.of(new ItemStack(Material.COD));
        }

        double categoryRoll = ThreadLocalRandom.current().nextDouble(totalWeight);

        if (categoryRoll < fishWeight) {
            return Optional.of(randomItem(this.config.fishLoot));
        }

        if (categoryRoll < fishWeight + junkWeight) {
            return Optional.of(randomItem(this.config.junkLoot));
        }

        return Optional.of(randomItem(this.config.treasureLoot));
    }

    private static ItemStack randomItem(
            Map<XMaterial, Integer> configuredLoot
    ) {
        if (configuredLoot == null || configuredLoot.isEmpty()) {
            return new ItemStack(Material.COD);
        }

        long totalWeight = 0L;

        for (Integer weight : configuredLoot.values()) {
            if (weight != null && weight > 0) {
                totalWeight += weight;
            }
        }

        if (totalWeight <= 0L) {
            return new ItemStack(Material.COD);
        }

        long materialRoll = ThreadLocalRandom.current().nextLong(totalWeight);

        for (Map.Entry<XMaterial, Integer> entry : configuredLoot.entrySet()) {
            Integer weight = entry.getValue();

            if (weight == null || weight <= 0) {
                continue;
            }

            if (materialRoll >= weight) {
                materialRoll -= weight;
                continue;
            }

            Material material = entry.getKey().parseMaterial();

            if (material != null) {
                return new ItemStack(material);
            }

            break;
        }

        return new ItemStack(Material.COD);
    }

    private WaterBodyScanner.ScanResult findFishingWater(
            MinionContext context
    ) {
        Minion minion = context.minion();

        return this.scanner.scan(
                context.world(),
                minion.position().blockX(),
                minion.position().blockY(),
                minion.position().blockZ(),
                this.config.searchRange(),
                this.config.searchDepth(),
                this.config.requiredWaterBlocks(),
                this.config.requiredWaterDepth(),
                this.config.requireOpenSurface
        );
    }

    private static MinionStatus status(
            WaterBodyScanner.Failure failure
    ) {
        return switch (failure) {
            case NO_WATER ->
                    FishermanStatuses.NO_WATER_NEARBY;

            case TOO_SMALL ->
                    FishermanStatuses.WATER_TOO_SMALL;

            case TOO_SHALLOW ->
                    FishermanStatuses.WATER_TOO_SHALLOW;

            case SURFACE_BLOCKED ->
                    FishermanStatuses.WATER_SURFACE_BLOCKED;
        };
    }

}
