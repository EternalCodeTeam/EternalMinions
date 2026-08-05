package com.eternalcode.minions.minion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable point-in-time view of every public piece of live minion state.
 * Item stacks are cloned on input and output, so callers cannot mutate plugin-owned inventory.
 */
public record MinionSnapshot(
    @NotNull MinionDetails details,
    long progress,
    @NotNull MinionDirection direction,
    @Nullable ItemStack tool,
    @NotNull List<@Nullable ItemStack> storageContents,
    int storageCapacity,
    @NotNull Map<String, Integer> upgradeTiers,
    @Nullable MinionPosition chestPosition,
    @NotNull String statusKey
) {

    public MinionSnapshot {
        if (details == null || direction == null) {
            throw new IllegalArgumentException("Minion details and direction are required");
        }
        if (progress < 0L) {
            throw new IllegalArgumentException("Minion progress cannot be negative");
        }
        if (storageCapacity < 1 || storageContents == null || storageContents.size() > storageCapacity) {
            throw new IllegalArgumentException("Storage contents must fit declared capacity");
        }
        if (upgradeTiers == null) {
            throw new IllegalArgumentException("Upgrade tiers are required");
        }
        if (statusKey == null || statusKey.isBlank()) {
            throw new IllegalArgumentException("Status key must not be blank");
        }

        tool = cloneItem(tool);
        storageContents = cloneItems(storageContents);
        upgradeTiers = Collections.unmodifiableMap(new LinkedHashMap<>(upgradeTiers));
    }

    @Override
    public @Nullable ItemStack tool() {
        return cloneItem(this.tool);
    }

    @Override
    public @NotNull List<@Nullable ItemStack> storageContents() {
        return cloneItems(this.storageContents);
    }

    private static @Nullable ItemStack cloneItem(@Nullable ItemStack item) {
        return item == null ? null : item.clone();
    }

    private static List<@Nullable ItemStack> cloneItems(List<@Nullable ItemStack> items) {
        List<@Nullable ItemStack> clones = new ArrayList<>(items.size());
        for (ItemStack item : items) {
            clones.add(cloneItem(item));
        }
        return Collections.unmodifiableList(clones);
    }
}
