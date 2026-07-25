package com.eternalcode.minions.minion.collector;

import com.eternalcode.minions.database.MinionPersistenceService;
import com.eternalcode.minions.minion.AbstractMinionBehavior;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionRegistry;
import com.eternalcode.minions.minion.MinionStorage;
import com.eternalcode.minions.minion.MinionStorageUpdate;
import com.eternalcode.minions.minion.MinionType;
import com.eternalcode.minions.minion.ScheduledMinion;
import com.eternalcode.minions.minion.status.CoreMinionStatuses;
import com.eternalcode.minions.minion.status.MinionStatus;
import com.eternalcode.minions.minion.status.MinionStatusTracker;
import com.eternalcode.minions.minion.tool.ToolCheck;
import com.eternalcode.minions.minion.tool.ToolInventoryLocator;
import com.eternalcode.minions.minion.tool.ToolValidationService;
import com.eternalcode.minions.render.MinionRenderer;
import java.util.Collection;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Container;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class CollectorBehavior extends AbstractMinionBehavior {

    private static final int MAX_SCANNED_ENTITIES_PER_ACTION = 64;
    private static final int MAX_COLLECTED_STACKS_PER_ACTION = 8;

    private final ToolValidationService toolValidation;
    private final ToolInventoryLocator toolLocator;

    public CollectorBehavior(
            MinionRegistry registry,
            MinionPersistenceService persistence,
            MinionRenderer renderer,
            MinionStatusTracker statuses,
            ToolValidationService toolValidation,
            ToolInventoryLocator toolLocator
    ) {
        super(
                registry,
                persistence,
                renderer,
                statuses
        );

        this.toolValidation = toolValidation;
        this.toolLocator = toolLocator;
    }

    private static int moveToInventory(
            Inventory inventory,
            ItemStack source,
            int amount
    ) {
        int remainingAmount = amount;
        int maximumStackSize = Math.min(
                source.getMaxStackSize(),
                inventory.getMaxStackSize()
        );

        remainingAmount = fillExistingStacks(
                inventory,
                source,
                remainingAmount,
                maximumStackSize
        );

        if (remainingAmount <= 0) {
            return 0;
        }

        return fillEmptySlots(
                inventory,
                source,
                remainingAmount,
                maximumStackSize
        );
    }

    private static int fillExistingStacks(
            Inventory inventory,
            ItemStack source,
            int amount,
            int maximumStackSize
    ) {
        int remainingAmount = amount;

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (remainingAmount <= 0) {
                break;
            }

            ItemStack current = inventory.getItem(slot);

            if (current == null || !current.isSimilar(source)) {
                continue;
            }

            int availableSpace = maximumStackSize - current.getAmount();

            if (availableSpace <= 0) {
                continue;
            }

            int transferredAmount = Math.min(
                    availableSpace,
                    remainingAmount
            );

            current.setAmount(
                    current.getAmount() + transferredAmount
            );

            inventory.setItem(
                    slot,
                    current
            );

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

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (remainingAmount <= 0) {
                break;
            }

            ItemStack current = inventory.getItem(slot);

            if (current != null && !current.getType().isAir()) {
                continue;
            }

            int transferredAmount = Math.min(
                    maximumStackSize,
                    remainingAmount
            );

            ItemStack inserted = source.clone();
            inserted.setAmount(transferredAmount);

            inventory.setItem(
                    slot,
                    inserted
            );

            remainingAmount -= transferredAmount;
        }

        return remainingAmount;
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
                CollectorStatuses.NO_SHOVEL
        );

        if (toolCheck instanceof ToolCheck.Stopped(MinionStatus reason)) {
            this.refreshStatusIfChanged(
                    preparedMinion,
                    reason
            );

            return false;
        }

        double radius = type.work().collectorRadius();
        Location center = this.minionLocation(
                preparedMinion,
                world
        );

        Container linkedChest = this.resolveLinkedChest(
                preparedMinion,
                world
        );

        Inventory chestInventory = linkedChest == null
                ? null
                : linkedChest.getInventory();

        MinionStorage storage = preparedMinion.storage();

        boolean foundCollectibleItem = false;
        boolean movedAnyItem = false;
        boolean destinationBlocked = false;

        int scannedEntities = 0;
        int collectedStacks = 0;

        Collection<Entity> nearbyEntities = world.getNearbyEntities(
                center,
                radius,
                radius,
                radius,
                entity -> entity instanceof Item
        );

        for (Entity entity : nearbyEntities) {
            if (scannedEntities >= MAX_SCANNED_ENTITIES_PER_ACTION) {
                break;
            }

            scannedEntities++;

            Item item = (Item) entity;
            ItemStack itemStack = item.getItemStack();

            if (itemStack.getAmount() <= 0) {
                continue;
            }

            if (!type.work().collectorAllows(itemStack.getType())) {
                continue;
            }

            foundCollectibleItem = true;

            if (collectedStacks >= MAX_COLLECTED_STACKS_PER_ACTION) {
                break;
            }

            collectedStacks++;

            int originalAmount = itemStack.getAmount();
            int remainingAmount = originalAmount;

            if (chestInventory != null) {
                remainingAmount = moveToInventory(
                        chestInventory,
                        itemStack,
                        remainingAmount
                );
            }

            if (remainingAmount > 0) {
                ItemStack storageCandidate = itemStack.clone();
                storageCandidate.setAmount(remainingAmount);

                MinionStorageUpdate update = storage.add(storageCandidate);

                storage = update.storage();

                ItemStack remaining = update.remaining();

                remainingAmount = remaining == null
                        ? 0
                        : remaining.getAmount();
            }

            int movedAmount = originalAmount - remainingAmount;

            if (movedAmount <= 0) {
                destinationBlocked = true;
                continue;
            }

            movedAnyItem = true;

            if (remainingAmount <= 0) {
                item.remove();
                continue;
            }

            itemStack.setAmount(remainingAmount);
            item.setItemStack(itemStack);

            destinationBlocked = true;
        }

        if (!foundCollectibleItem) {
            this.refreshStatusIfChanged(
                    preparedMinion,
                    CollectorStatuses.NO_ITEMS_ON_GROUND
            );

            return false;
        }

        if (!movedAnyItem) {
            this.refreshStatusIfChanged(
                    preparedMinion,
                    CoreMinionStatuses.STORAGE_FULL
            );

            return false;
        }

        this.commit(
                preparedMinion,
                type,
                storage,
                Float.NaN
        );

        this.refreshStatusIfChanged(
                preparedMinion,
                destinationBlocked
                        ? CoreMinionStatuses.STORAGE_FULL
                        : CollectorStatuses.COLLECTING
        );

        return true;
    }
}