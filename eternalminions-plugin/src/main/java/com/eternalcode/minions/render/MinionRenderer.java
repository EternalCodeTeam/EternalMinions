package com.eternalcode.minions.render;

import com.eternalcode.minions.config.MinionsConfig;
import com.eternalcode.minions.item.MinionAppearanceItems;
import com.eternalcode.minions.minion.MinionBehaviorRegistry;
import com.eternalcode.minions.minion.MinionId;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.status.MinionStatusTracker;
import com.github.retrooper.packetevents.PacketEvents;
import me.tofaa.entitylib.APIConfig;
import me.tofaa.entitylib.EntityLib;
import me.tofaa.entitylib.spigot.SpigotEntityLibPlatform;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public interface MinionRenderer {

    static MinionRenderer create(
        JavaPlugin plugin,
        MinionsConfig config,
        MiniMessage miniMessage,
        MinionBehaviorRegistry behaviors,
        MinionStatusTracker statusTracker,
        MinionEntityIndex entityIndex,
        MinionAppearanceItems appearance
    ) {
        EntityLib.init(
                new SpigotEntityLibPlatform(plugin),
                new APIConfig(PacketEvents.getAPI()).usePlatformLogger()
        );

        EntityLibHologramRenderer holograms = new EntityLibHologramRenderer(
                plugin.getServer(),
                miniMessage,
                behaviors,
                config,
                statusTracker
        );

        return switch (config.minionRenderer) {
            case ARMOR_STAND -> new ArmorStandMinionRenderer(holograms, entityIndex, behaviors, appearance);
            case NPC -> new NpcMinionRenderer(holograms, entityIndex, behaviors, appearance);
        };
    }

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
