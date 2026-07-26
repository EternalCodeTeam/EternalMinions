package com.eternalcode.minions.minion.impl.collector;

import com.eternalcode.minions.config.AbstractMinionConfig;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionBehavior;
import com.eternalcode.minions.minion.MinionContext;
import com.eternalcode.minions.minion.MinionResult;
import com.eternalcode.minions.minion.storage.MinionStorage;
import com.eternalcode.minions.minion.storage.MinionStorageUpdate;
import com.eternalcode.minions.minion.status.CoreMinionStatuses;
import com.eternalcode.minions.minion.tool.ToolCheck;
import com.eternalcode.minions.minion.tool.ToolInventoryLocator;
import com.eternalcode.minions.minion.tool.ToolRequirement;
import com.eternalcode.minions.minion.tool.ToolValidationService;
import java.util.Collection;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Container;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class CollectorBehavior implements MinionBehavior {

    private static final int MAX_SCANNED_ENTITIES_PER_ACTION = 64;
    private static final int MAX_COLLECTED_STACKS_PER_ACTION = 8;

    private final CollectorConfig config;
    private final ToolValidationService toolValidation;
    private final ToolInventoryLocator toolLocator;
    private final ToolRequirement toolRequirement;
    private final Set<Material> allowedMaterials;
    private final Set<Material> blockedMaterials;

    public CollectorBehavior(
        CollectorConfig config,
        ToolValidationService toolValidation,
        ToolInventoryLocator toolLocator
    ) {
        this.config = config;
        this.toolValidation = toolValidation;
        this.toolLocator = toolLocator;
        this.toolRequirement = config.toolRequirement();
        this.allowedMaterials = config.materials(config.collectorAllowedMaterials);
        this.blockedMaterials = config.materials(config.collectorBlockedMaterials);
        if (config.collectorRadiusBlocks < 1 || config.collectorRadiusBlocks > 16) {
            throw new IllegalArgumentException("Collector radius must be between 1 and 16");
        }
    }

    @Override
    public String id() {
        return "collector";
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
            CollectorStatuses.NO_SHOVEL
        );
        if (toolCheck instanceof ToolCheck.Stopped stopped) {
            return MinionResult.idle(minion, stopped.reason());
        }

        Location center = context.location();
        Container linkedChest = context.linkedChest();
        Inventory chestInventory = linkedChest == null ? null : linkedChest.getInventory();
        MinionStorage storage = minion.storage();
        boolean foundItem = false;
        boolean movedItem = false;
        boolean destinationBlocked = false;
        int scannedEntities = 0;
        int collectedStacks = 0;

        Collection<Entity> nearbyEntities = context.world().getNearbyEntities(
            center,
            this.config.collectorRadiusBlocks,
            this.config.collectorRadiusBlocks,
            this.config.collectorRadiusBlocks,
            entity -> entity instanceof Item
        );
        for (Entity entity : nearbyEntities) {
            if (scannedEntities++ >= MAX_SCANNED_ENTITIES_PER_ACTION) {
                break;
            }

            Item item = (Item) entity;
            ItemStack itemStack = item.getItemStack();
            if (itemStack.getAmount() <= 0 || !this.allows(itemStack.getType())) {
                continue;
            }
            foundItem = true;
            if (collectedStacks++ >= MAX_COLLECTED_STACKS_PER_ACTION) {
                break;
            }

            int originalAmount = itemStack.getAmount();
            int remainingAmount = originalAmount;
            if (chestInventory != null) {
                remainingAmount = moveToInventory(chestInventory, itemStack, remainingAmount);
            }
            if (remainingAmount > 0) {
                ItemStack storageCandidate = itemStack.clone();
                storageCandidate.setAmount(remainingAmount);
                MinionStorageUpdate update = storage.add(storageCandidate);
                storage = update.storage();
                remainingAmount = update.remaining() == null ? 0 : update.remaining().getAmount();
            }

            if (remainingAmount == originalAmount) {
                destinationBlocked = true;
                continue;
            }
            movedItem = true;
            if (remainingAmount <= 0) {
                item.remove();
                continue;
            }
            itemStack.setAmount(remainingAmount);
            item.setItemStack(itemStack);
            destinationBlocked = true;
        }

        if (!foundItem) {
            return MinionResult.idle(minion, CollectorStatuses.NO_ITEMS_ON_GROUND);
        }
        if (!movedItem) {
            return MinionResult.idle(minion, CoreMinionStatuses.STORAGE_FULL);
        }

        Minion updated = minion.withStorage(storage);
        updated = updated.withProgress(updated.progress().advanced(this.config));
        return MinionResult.worked(
            updated,
            destinationBlocked ? CoreMinionStatuses.STORAGE_FULL : CollectorStatuses.COLLECTING
        );
    }

    private boolean allows(Material material) {
        if (this.blockedMaterials.contains(material)) {
            return false;
        }
        return this.allowedMaterials.isEmpty() || this.allowedMaterials.contains(material);
    }

    private static int moveToInventory(Inventory inventory, ItemStack source, int amount) {
        int maximumStackSize = Math.min(source.getMaxStackSize(), inventory.getMaxStackSize());
        int remainingAmount = fillExistingStacks(inventory, source, amount, maximumStackSize);
        if (remainingAmount <= 0) {
            return 0;
        }
        return fillEmptySlots(inventory, source, remainingAmount, maximumStackSize);
    }

    private static int fillExistingStacks(
        Inventory inventory,
        ItemStack source,
        int amount,
        int maximumStackSize
    ) {
        int remainingAmount = amount;
        for (int slot = 0; slot < inventory.getSize() && remainingAmount > 0; slot++) {
            ItemStack current = inventory.getItem(slot);
            if (current == null || !current.isSimilar(source)) {
                continue;
            }
            int availableSpace = maximumStackSize - current.getAmount();
            if (availableSpace <= 0) {
                continue;
            }
            int transferredAmount = Math.min(availableSpace, remainingAmount);
            current.setAmount(current.getAmount() + transferredAmount);
            inventory.setItem(slot, current);
            remainingAmount -= transferredAmount;
        }
        return remainingAmount;
    }

    private static int fillEmptySlots(
        Inventory inventory,
        ItemStack source,
        int amount,
        int maximumStackSize
    ) {
        int remainingAmount = amount;
        for (int slot = 0; slot < inventory.getSize() && remainingAmount > 0; slot++) {
            ItemStack current = inventory.getItem(slot);
            if (current != null && !current.getType().isAir()) {
                continue;
            }
            int transferredAmount = Math.min(maximumStackSize, remainingAmount);
            ItemStack inserted = source.clone();
            inserted.setAmount(transferredAmount);
            inventory.setItem(slot, inserted);
            remainingAmount -= transferredAmount;
        }
        return remainingAmount;
    }

}
