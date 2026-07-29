package com.eternalcode.minions.minion.storage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.bukkit.inventory.ItemStack;

final class MinionItemStacks {

    private MinionItemStacks() {
    }

    static List<ItemStack> combine(Collection<ItemStack> items) {
        if (items == null) {
            throw new IllegalArgumentException("Items are required");
        }

        List<ItemStack> combined = new ArrayList<>(items.size());
        for (ItemStack item : items) {
            if (item == null || item.getType().isAir() || item.getAmount() <= 0) {
                continue;
            }

            add(combined, item);
        }

        return List.copyOf(combined);
    }

    static boolean requiresCombine(Collection<ItemStack> items) {
        if (items == null) {
            throw new IllegalArgumentException("Items are required");
        }
        if (items.size() != 1) {
            return items.size() > 1;
        }

        ItemStack item = items.iterator().next();
        return item != null && requiresCombine(1, item.getAmount(), item.getMaxStackSize());
    }

    static boolean requiresCombine(int itemCount, int itemAmount, int maxStackSize) {
        if (itemCount < 0) {
            throw new IllegalArgumentException("Item count must not be negative");
        }
        if (maxStackSize < 1) {
            throw new IllegalArgumentException("Max stack size must be positive");
        }

        return itemCount > 1 || itemCount == 1 && itemAmount > maxStackSize;
    }

    static int stackCount(int totalAmount, int maxStackSize) {
        if (maxStackSize < 1) {
            throw new IllegalArgumentException("Max stack size must be positive");
        }
        if (totalAmount <= 0) {
            return 0;
        }

        return (totalAmount + maxStackSize - 1) / maxStackSize;
    }

    static int stackAmount(int totalAmount, int maxStackSize, int stackIndex) {
        if (stackIndex < 0) {
            throw new IllegalArgumentException("Stack index must not be negative");
        }

        int remainingAmount = totalAmount - stackIndex * maxStackSize;
        return Math.max(0, Math.min(maxStackSize, remainingAmount));
    }

    private static void add(List<ItemStack> combined, ItemStack item) {
        int remainingAmount = item.getAmount();
        for (ItemStack existing : combined) {
            if (!existing.isSimilar(item)) {
                continue;
            }

            int freeAmount = existing.getMaxStackSize() - existing.getAmount();
            if (freeAmount <= 0) {
                continue;
            }

            int addedAmount = Math.min(freeAmount, remainingAmount);
            existing.setAmount(existing.getAmount() + addedAmount);
            remainingAmount -= addedAmount;
            if (remainingAmount == 0) {
                return;
            }
        }

        int maxStackSize = item.getMaxStackSize();
        int stackCount = stackCount(remainingAmount, maxStackSize);
        for (int stackIndex = 0; stackIndex < stackCount; stackIndex++) {
            ItemStack stack = item.clone();
            stack.setAmount(stackAmount(remainingAmount, maxStackSize, stackIndex));
            combined.add(stack);
        }
    }
}
