package com.eternalcode.minions.item;

import com.eternalcode.minions.minion.MinionId;
import java.util.Optional;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/** API for portable minion items used by commands, rewards and custom GUIs. */
public interface MinionItemService {

    /** Creates a fresh level-one item for an enabled behavior. */
    @NotNull ItemStack create(@NotNull String behaviorId);

    /** Creates an item carrying the complete portable state of a live minion. */
    @NotNull Optional<ItemStack> createFromMinion(@NotNull MinionId minionId);

    /** Returns whether an item was created by EternalMinions. */
    boolean isMinionItem(@NotNull ItemStack item);

    /** Reads portable state from a minion item without modifying it. */
    @NotNull Optional<MinionItemSnapshot> inspect(@NotNull ItemStack item);
}
