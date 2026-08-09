package com.eternalcode.minions.minion.behavior;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

public final class BlockDrops {

    private BlockDrops() {
    }

    public static List<ItemStack> collect(Block block, ItemStack tool) {
        List<ItemStack> destination = new ArrayList<>();
        collectInto(block, tool, destination);
        return destination;
    }

    public static void collectInto(Block block, ItemStack tool, List<ItemStack> destination) {
        Collection<ItemStack> drops = tool == null ? block.getDrops() : block.getDrops(tool);
        for (ItemStack drop : drops) {
            if (drop == null || drop.getType().isAir() || drop.getAmount() <= 0) {
                continue;
            }
            destination.add(drop.clone());
        }
    }
}
