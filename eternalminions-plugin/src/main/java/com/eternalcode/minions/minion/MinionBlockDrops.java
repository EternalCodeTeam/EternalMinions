package com.eternalcode.minions.minion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

// Shared drop resolution for minions that break blocks (Fortune/Silk Touch are respected because
// the tool's ItemStack is passed straight into Block#getDrops).
public final class MinionBlockDrops {

    private MinionBlockDrops() {
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
