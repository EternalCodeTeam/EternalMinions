package com.eternalcode.minions.minion.tool;

import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionContext;
import com.eternalcode.minions.minion.storage.MinionItemTransferService;
import com.eternalcode.minions.minion.storage.MinionStorage;
import com.eternalcode.minions.minion.storage.MinionStorageUpdate;
import com.eternalcode.minions.minion.status.MinionStatus;
import java.util.List;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.inventory.ItemStack;

public final class MinionToolService {

    private final ToolValidationService validation;
    private final ToolDurabilityService durability;
    private final ToolInventoryLocator locator;
    private final MinionItemTransferService transfers;

    public MinionToolService(
            ToolValidationService validation,
            ToolDurabilityService durability,
            ToolInventoryLocator locator,
            MinionItemTransferService transfers
    ) {
        if (validation == null || durability == null || locator == null || transfers == null) {
            throw new IllegalArgumentException("Tool service dependencies are required");
        }

        this.validation = validation;
        this.durability = durability;
        this.locator = locator;
        this.transfers = transfers;
    }

    public MinionToolPreparation prepare(
            MinionContext context,
            ToolRequirement requirement,
            MinionStatus missingToolStatus
    ) {
        if (context == null || requirement == null || missingToolStatus == null) {
            throw new IllegalArgumentException("Tool preparation arguments are required");
        }

        Minion minion = this.retireWornTool(context, context.minion(), requirement);
        ToolCheck check = this.validation.validate(
                requirement,
                minion.equipment().tool(),
                missingToolStatus
        );
        return new MinionToolPreparation(minion, check);
    }

    public ToolCheck validateAgainstBlock(
            ToolRequirement requirement,
            ItemStack tool,
            Block target,
            MinionStatus weakToolStatus
    ) {
        return this.validation.validateAgainstBlock(requirement, tool, target, weakToolStatus);
    }

    public Minion consume(Minion minion, int uses) {
        if (minion == null) {
            throw new IllegalArgumentException("Minion is required");
        }
        if (uses < 1) {
            return minion;
        }

        ItemStack tool = minion.equipment().tool();
        if (tool == null) {
            return minion;
        }

        ItemStack damagedTool = this.durability.consume(tool, uses);
        return minion.withEquipment(minion.equipment().withDurability(damagedTool));
    }

    private Minion retireWornTool(
            MinionContext context,
            Minion minion,
            ToolRequirement requirement
    ) {
        ItemStack tool = minion.equipment().tool();
        if (tool == null || this.validation.remainingDurability(tool) > requirement.minDurabilityToKeep()) {
            return minion;
        }
        if (!this.transfers.canStoreAll(context, minion.storage(), List.of(tool))) {
            return minion;
        }

        MinionStorageUpdate storedTool = this.transfers.store(context, minion.storage(), tool);
        ItemStack overflow = storedTool.remaining();
        if (overflow != null) {
            context.world().dropItemNaturally(context.location(), overflow);
        }

        MinionStorage storage = storedTool.storage();
        ItemStack replacement = null;
        int storageSlot = this.locator.findInStorage(storage.snapshot(), requirement);
        if (storageSlot >= 0) {
            replacement = storage.item(storageSlot);
            storage = storage.withItem(storageSlot, null);
        }
        else {
            Container chest = context.linkedChest();
            if (chest != null) {
                int chestSlot = this.locator.findInInventory(chest.getInventory(), requirement);
                if (chestSlot >= 0) {
                    replacement = chest.getInventory().getItem(chestSlot);
                    chest.getInventory().setItem(chestSlot, null);
                }
            }
        }

        return minion
                .withEquipment(minion.equipment().withTool(replacement))
                .withStorage(storage);
    }
}
