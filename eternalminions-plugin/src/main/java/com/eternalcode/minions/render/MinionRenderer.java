package com.eternalcode.minions.render;

import com.eternalcode.minions.minion.MinionId;
import com.eternalcode.minions.minion.Minion;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface MinionRenderer {

    void show(Player player, Minion minion);

    void hide(Player player, MinionId minionId);

    void remove(MinionId minionId);

    void animate(MinionId minionId, float targetYaw);

    default void refreshEquipment(MinionId minionId, ItemStack tool) {
    }

    default void refreshHologram(Minion minion) {
    }

    default void refreshRotation(Minion minion) {
    }

    default void tick(long currentTick) {
    }
}
