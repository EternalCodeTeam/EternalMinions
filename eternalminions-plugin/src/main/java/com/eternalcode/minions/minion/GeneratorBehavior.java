package com.eternalcode.minions.minion;

import com.eternalcode.minions.database.MinionPersistenceService;
import com.eternalcode.minions.render.MinionRenderer;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

// Hypixel-style resource generator: each action rolls the type's drop table into storage.
// Shared by LUMBERJACK, FARMER, FISHERMAN, KILLER and CRAFTER — none of them need profession-specific logic.
public final class GeneratorBehavior extends AbstractMinionBehavior {

    // Reused across actions on the single scheduler thread to avoid per-action allocation.
    private final List<ItemStack> produced = new ArrayList<>();

    public GeneratorBehavior(MinionRegistry registry, MinionPersistenceService persistence, MinionRenderer renderer) {
        super(registry, persistence, renderer);
    }

    @Override
    public boolean execute(Minion minion, MinionType type, ScheduledMinion scheduledMinion, World world) {
        MinionDrop[] drops = type.work().drops();
        if (drops.length == 0 || !this.hasStorageRoom(minion, world)) {
            return false;
        }
        if (type.work().requiresWater() && !this.hasWaterNearby(minion, world)) {
            return false;
        }

        this.produced.clear();
        for (MinionDrop drop : drops) {
            drop.roll(this.produced);
        }

        this.deposit(minion, type, world, this.minionLocation(minion, world), this.produced, Float.NaN);
        return true;
    }

    private boolean hasWaterNearby(Minion minion, World world) {
        int baseX = minion.position().blockX();
        int baseY = minion.position().blockY();
        int baseZ = minion.position().blockZ();
        if (!world.isChunkLoaded(baseX >> 4, baseZ >> 4)) {
            return false;
        }

        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                if (this.isWater(world.getBlockAt(baseX + offsetX, baseY, baseZ + offsetZ))
                    || this.isWater(world.getBlockAt(baseX + offsetX, baseY - 1, baseZ + offsetZ))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isWater(Block block) {
        return block.getType() == Material.WATER;
    }
}
