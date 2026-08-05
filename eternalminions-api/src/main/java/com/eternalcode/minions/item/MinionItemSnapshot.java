package com.eternalcode.minions.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Immutable state encoded inside a portable minion item. */
public record MinionItemSnapshot(
    @NotNull String behaviorId,
    int level,
    long progress,
    @Nullable ItemStack tool,
    @NotNull List<@Nullable ItemStack> storageContents,
    @NotNull Map<String, Integer> upgradeTiers
) {

    public MinionItemSnapshot {
        if (behaviorId == null || behaviorId.isBlank()) {
            throw new IllegalArgumentException("Behavior id must not be blank");
        }
        if (level < 1 || progress < 0L) {
            throw new IllegalArgumentException("Item progression is invalid");
        }
        if (storageContents == null || upgradeTiers == null) {
            throw new IllegalArgumentException("Item storage and upgrades are required");
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
