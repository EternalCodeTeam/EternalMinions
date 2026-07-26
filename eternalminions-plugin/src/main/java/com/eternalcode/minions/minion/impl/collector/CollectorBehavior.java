package com.eternalcode.minions.minion.impl.collector;

import com.eternalcode.minions.config.AbstractMinionConfig;
import com.eternalcode.minions.config.ConfigService;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionBehavior;
import com.eternalcode.minions.minion.MinionContext;
import com.eternalcode.minions.minion.MinionResult;
import com.eternalcode.minions.minion.MaterialFilter;
import com.eternalcode.minions.minion.storage.MinionItemTransferService;
import com.eternalcode.minions.minion.storage.MinionStorage;
import com.eternalcode.minions.minion.storage.MinionStorageUpdate;
import com.eternalcode.minions.minion.status.CoreMinionStatuses;
import com.eternalcode.minions.minion.tool.MinionToolPreparation;
import com.eternalcode.minions.minion.tool.MinionToolService;
import com.eternalcode.minions.minion.tool.ToolCheck;
import com.eternalcode.minions.minion.tool.ToolRequirement;
import java.io.File;
import java.util.Collection;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

public final class CollectorBehavior implements MinionBehavior {

    private static final int MAX_SCANNED_ENTITIES_PER_ACTION = 64;
    private static final int MAX_COLLECTED_STACKS_PER_ACTION = 8;

    private final CollectorConfig config;
    private final MinionToolService tools;
    private final MinionItemTransferService transfers;
    private final ToolRequirement toolRequirement;
    private final MaterialFilter materialFilter;

    public static CollectorBehavior create(
        ConfigService configs,
        File directory,
        MinionToolService tools,
        MinionItemTransferService transfers
    ) {
        CollectorConfig config = configs.load(CollectorConfig.class, new File(directory, "collector.yml"));
        return new CollectorBehavior(config, tools, transfers);
    }

    public CollectorBehavior(
        CollectorConfig config,
        MinionToolService tools,
        MinionItemTransferService transfers
    ) {
        this.config = config;
        this.tools = tools;
        this.transfers = transfers;
        this.toolRequirement = config.toolRequirement();
        this.materialFilter = new MaterialFilter(
                config.materials(config.collectorAllowedMaterials),
                config.materials(config.collectorBlockedMaterials)
        );
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
        MinionToolPreparation preparation = this.tools.prepare(
            context,
            this.toolRequirement,
            CollectorStatuses.NO_SHOVEL
        );
        if (preparation.check() instanceof ToolCheck.Stopped stopped) {
            return MinionResult.idle(preparation.minion(), stopped.reason());
        }
        Minion minion = preparation.minion();

        Location center = context.location();
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
            MinionStorageUpdate update = this.transfers.store(context, storage, itemStack);
            storage = update.storage();
            ItemStack remaining = update.remaining();
            int remainingAmount = remaining == null ? 0 : remaining.getAmount();

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
        return this.materialFilter.allows(material);
    }

}
