package com.eternalcode.minions.minion.impl.fisherman;

import com.eternalcode.minions.config.AbstractMinionConfig;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionBehavior;
import com.eternalcode.minions.minion.MinionContext;
import com.eternalcode.minions.minion.MinionResult;
import com.eternalcode.minions.minion.status.CoreMinionStatuses;
import com.eternalcode.minions.minion.tool.ToolCheck;
import com.eternalcode.minions.minion.tool.ToolDurabilityService;
import com.eternalcode.minions.minion.tool.ToolInventoryLocator;
import com.eternalcode.minions.minion.tool.ToolRequirement;
import com.eternalcode.minions.minion.tool.ToolValidationService;
import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.LootTables;

public final class FishermanBehavior implements MinionBehavior {

    private final FishermanConfig config;
    private final Server server;
    private final ToolValidationService toolValidation;
    private final ToolDurabilityService toolDurability;
    private final ToolInventoryLocator toolLocator;
    private final ToolRequirement toolRequirement;
    private final WaterBodyScanner scanner = new WaterBodyScanner();

    public FishermanBehavior(
        FishermanConfig config,
        Server server,
        ToolValidationService toolValidation,
        ToolDurabilityService toolDurability,
        ToolInventoryLocator toolLocator
    ) {
        this.config = config;
        this.server = server;
        this.toolValidation = toolValidation;
        this.toolDurability = toolDurability;
        this.toolLocator = toolLocator;
        this.toolRequirement = config.toolRequirement();
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
        Minion minion = context.retireWornTool(
            context.minion(),
            this.toolRequirement,
            this.toolValidation,
            this.toolLocator
        );
        ToolCheck toolCheck = this.toolValidation.validate(
            this.toolRequirement,
            minion.equipment().tool(),
            FisherStatuses.NO_ROD
        );
        if (toolCheck instanceof ToolCheck.Stopped stopped) {
            context.scheduledMinion().clearBusyTimer();
            return MinionResult.idle(minion, stopped.reason());
        }
        if (!context.hasStorageRoom()) {
            context.scheduledMinion().clearBusyTimer();
            return MinionResult.idle(minion, CoreMinionStatuses.STORAGE_FULL);
        }

        Block waterBlock = this.findWaterNearby(context);
        if (waterBlock == null) {
            context.scheduledMinion().clearBusyTimer();
            return MinionResult.idle(minion, FisherStatuses.NO_WATER_NEARBY);
        }
        int connectedWaterBlocks = this.scanner.countConnected(
            waterBlock.getX(),
            waterBlock.getY(),
            waterBlock.getZ(),
            (blockX, blockY, blockZ) ->
                context.world().getBlockAt(blockX, blockY, blockZ).getType() == Material.WATER,
            this.config.minWaterBlocks
        );
        if (connectedWaterBlocks < this.config.minWaterBlocks) {
            context.scheduledMinion().clearBusyTimer();
            return MinionResult.idle(minion, FisherStatuses.WATER_TOO_SMALL);
        }

        ItemStack tool = minion.equipment().tool();
        long worldTime = context.world().getFullTime();
        if (context.scheduledMinion().isBusyUntil(worldTime)) {
            long remainingTicks = context.scheduledMinion().remainingBusyTicks(worldTime);
            return MinionResult.idle(minion, FisherStatuses.FISHING).withDelay(remainingTicks);
        }
        if (!context.scheduledMinion().hasBusyTimer()) {
            long waitTicks = this.fishingWait(tool);
            context.scheduledMinion().busyUntil(worldTime + waitTicks);
            return MinionResult.worked(minion, FisherStatuses.FISHING).withDelay(waitTicks);
        }

        context.scheduledMinion().clearBusyTimer();
        int luck = tool == null ? 0 : tool.getEnchantmentLevel(Enchantment.LUCK_OF_THE_SEA);
        LootContext lootContext = new LootContext.Builder(waterBlock.getLocation()).luck(luck).build();
        LootTable lootTable = this.server.getLootTable(LootTables.FISHING.getKey());
        if (lootTable == null) {
            return MinionResult.idle(minion, FisherStatuses.NO_WATER_NEARBY);
        }

        Collection<ItemStack> drops = lootTable.populateLoot(ThreadLocalRandom.current(), lootContext);
        Minion updated = this.consumeTool(minion, tool);
        updated = context.deposit(updated, waterBlock.getLocation(), drops);
        updated = updated.withProgress(updated.progress().advanced(this.config));
        return MinionResult.worked(updated, CoreMinionStatuses.WORKING);
    }

    private long fishingWait(ItemStack tool) {
        int lureLevel = tool == null ? 0 : tool.getEnchantmentLevel(Enchantment.LURE);
        return Math.max(
            1L,
            this.config.baseWaitTicks - (long) this.config.lureTicksReductionPerLevel * lureLevel
        );
    }

    private Block findWaterNearby(MinionContext context) {
        int baseX = context.minion().position().blockX();
        int baseY = context.minion().position().blockY();
        int baseZ = context.minion().position().blockZ();
        if (!context.world().isChunkLoaded(baseX >> 4, baseZ >> 4)) {
            return null;
        }

        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                Block sameLevel = context.world().getBlockAt(baseX + offsetX, baseY, baseZ + offsetZ);
                if (sameLevel.getType() == Material.WATER) {
                    return sameLevel;
                }
                Block below = context.world().getBlockAt(baseX + offsetX, baseY - 1, baseZ + offsetZ);
                if (below.getType() == Material.WATER) {
                    return below;
                }
            }
        }
        return null;
    }

    private Minion consumeTool(Minion minion, ItemStack tool) {
        if (tool == null) {
            return minion;
        }
        ItemStack damagedTool = this.toolDurability.consume(tool, 1);
        return minion.withEquipment(minion.equipment().withTool(damagedTool));
    }

}
