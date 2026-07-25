package com.eternalcode.minions.minion.fisherman;

import com.eternalcode.minions.database.MinionPersistenceService;
import com.eternalcode.minions.minion.AbstractMinionBehavior;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionRegistry;
import com.eternalcode.minions.minion.MinionType;
import com.eternalcode.minions.minion.ScheduledMinion;
import com.eternalcode.minions.minion.status.CoreMinionStatuses;
import com.eternalcode.minions.minion.status.MinionStatusTracker;
import com.eternalcode.minions.minion.tool.ToolCheck;
import com.eternalcode.minions.minion.tool.ToolDurabilityService;
import com.eternalcode.minions.minion.tool.ToolInventoryLocator;
import com.eternalcode.minions.minion.tool.ToolValidationService;
import com.eternalcode.minions.render.MinionRenderer;
import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.LootTables;

// Fishes at a real nearby water body: waits out a real elapsed-tick cast (shortened by Lure),
// then resolves the catch through Bukkit's actual vanilla LootTables.FISHING table with a luck
// value from Luck of the Sea, so the loot itself is vanilla-accurate and enchant-aware.
public final class FishermanBehavior extends AbstractMinionBehavior {

    private final ToolValidationService toolValidation;
    private final ToolDurabilityService toolDurability;
    private final ToolInventoryLocator toolLocator;
    private final WaterBodyScanner scanner = new WaterBodyScanner();

    public FishermanBehavior(
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
            type.work().toolRequirement(), minion.equipment().tool(), FisherStatuses.NO_ROD);
        if (toolCheck instanceof ToolCheck.Stopped stopped) {
            this.refreshStatusIfChanged(minion, stopped.reason());
            return false;
        }

        if (!this.hasStorageRoom(minion, world)) {
            this.refreshStatusIfChanged(minion, CoreMinionStatuses.STORAGE_FULL);
            return false;
        }

        FisherWork work = type.work().fisher();
        Block waterBlock = this.findWaterNearby(minion, world);
        if (waterBlock == null) {
            this.refreshStatusIfChanged(minion, FisherStatuses.NO_WATER_NEARBY);
            return false;
        }
        int connected = this.scanner.countConnected(
            waterBlock.getX(), waterBlock.getY(), waterBlock.getZ(),
            (x, y, z) -> world.getBlockAt(x, y, z).getType() == Material.WATER,
            work.minWaterBlocks()
        );
        if (connected < work.minWaterBlocks()) {
            this.refreshStatusIfChanged(minion, FisherStatuses.WATER_TOO_SMALL);
            return false;
        }

        ItemStack tool = minion.equipment().tool();
        long worldTime = world.getFullTime();
        if (scheduledMinion.isBusyUntil(worldTime)) {
            this.refreshStatusIfChanged(minion, FisherStatuses.FISHING);
            return false;
        }
        if (!this.currentStatus(minion).equals(FisherStatuses.FISHING)) {
            int lureLevel = tool == null ? 0 : tool.getEnchantmentLevel(Enchantment.LURE);
            long wait = Math.max(1L, work.baseWaitTicks() - (long) work.lureTicksReductionPerLevel() * lureLevel);
            scheduledMinion.busyUntil(worldTime + wait);
            this.refreshStatusIfChanged(minion, FisherStatuses.FISHING);
            return true;
        }

        int luck = tool == null ? 0 : tool.getEnchantmentLevel(Enchantment.LUCK_OF_THE_SEA);
        LootContext context = new LootContext.Builder(waterBlock.getLocation()).luck(luck).build();
        LootTable lootTable = Bukkit.getLootTable(LootTables.FISHING.getKey());
        Collection<ItemStack> drops = lootTable.populateLoot(ThreadLocalRandom.current(), context);

        Minion updated = this.consumeTool(minion, tool);
        this.deposit(updated, type, world, waterBlock.getLocation(), drops, Float.NaN);
        this.refreshStatusIfChanged(updated, CoreMinionStatuses.WORKING);
        return true;
    }

    private Block findWaterNearby(Minion minion, World world) {
        int baseX = minion.position().blockX();
        int baseY = minion.position().blockY();
        int baseZ = minion.position().blockZ();
        if (!world.isChunkLoaded(baseX >> 4, baseZ >> 4)) {
            return null;
        }
        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                Block sameLevel = world.getBlockAt(baseX + offsetX, baseY, baseZ + offsetZ);
                if (sameLevel.getType() == Material.WATER) {
                    return sameLevel;
                }
                Block below = world.getBlockAt(baseX + offsetX, baseY - 1, baseZ + offsetZ);
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
        Minion updated = minion.withEquipment(minion.equipment().withTool(damagedTool));
        this.registry.replace(updated);
        this.persistence.saveEquipment(updated);
        this.renderer.refreshEquipment(updated.id(), damagedTool);
        return updated;
    }
}
