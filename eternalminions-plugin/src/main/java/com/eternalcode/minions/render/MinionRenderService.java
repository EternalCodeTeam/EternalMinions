package com.eternalcode.minions.render;

import com.eternalcode.minions.config.MinionsConfig;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionId;
import com.eternalcode.minions.minion.MinionRegistry;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongConsumer;
import org.bukkit.Server;
import org.bukkit.entity.Player;

public final class MinionRenderService {

    private final Server server;
    private final MinionRegistry minions;
    private final MinionRenderer renderer;
    private final double renderDistanceSquared;
    private final int renderChunkRadius;
    private final Map<UUID, LongOpenHashSet> visible = new HashMap<>();

    public MinionRenderService(Server server, MinionRegistry minions, MinionRenderer renderer, MinionsConfig config) {
        this.server = server;
        this.minions = minions;
        this.renderer = renderer;
        this.renderDistanceSquared = (double) config.renderDistanceBlocks * config.renderDistanceBlocks;
        this.renderChunkRadius = (config.renderDistanceBlocks + 15) >> 4;
    }

    public void reconcile(Player player) {
        LongOpenHashSet playerVisible = this.visible.computeIfAbsent(player.getUniqueId(), ignored -> new LongOpenHashSet());
        int playerChunkX = player.getChunk().getX();
        int playerChunkZ = player.getChunk().getZ();
        String worldKey = player.getWorld().getKey().asString();
        LongConsumer showIfVisible = minionId -> {
            Minion minion = this.minions.findMinion(new MinionId(minionId)).orElse(null);
            if (minion == null || !this.isVisible(player, minion)) {
                return;
            }
            if (playerVisible.add(minionId)) {
                this.renderer.show(player, minion);
            }
        };
        for (int chunkX = playerChunkX - this.renderChunkRadius; chunkX <= playerChunkX + this.renderChunkRadius; chunkX++) {
            for (int chunkZ = playerChunkZ - this.renderChunkRadius; chunkZ <= playerChunkZ + this.renderChunkRadius; chunkZ++) {
                this.minions.forEachMinionIdInChunk(worldKey, chunkX, chunkZ, showIfVisible);
            }
        }

        LongIterator iterator = playerVisible.iterator();
        while (iterator.hasNext()) {
            long minionId = iterator.nextLong();
            Minion minion = this.minions.findMinion(new MinionId(minionId)).orElse(null);
            if (minion != null && this.isVisible(player, minion)) {
                continue;
            }
            this.renderer.hide(player, new MinionId(minionId));
            iterator.remove();
        }
    }

    public void showToNearby(Minion minion) {
        for (Player player : this.server.getOnlinePlayers()) {
            if (this.isVisible(player, minion)) {
                this.reconcile(player);
            }
        }
    }

    public void remove(Minion minion) {
        this.renderer.remove(minion.id());
        for (LongOpenHashSet playerVisible : this.visible.values()) {
            playerVisible.remove(minion.id().value());
        }
    }

    public void refreshEquipment(Minion minion) {
        this.renderer.refreshEquipment(minion.id(), minion.equipment().tool());
    }

    public void refreshRotation(Minion minion) {
        this.renderer.refreshRotation(minion);
    }

    public void hideAll(Player player) {
        LongOpenHashSet playerVisible = this.visible.remove(player.getUniqueId());
        if (playerVisible == null) {
            return;
        }
        for (long minionId : playerVisible) {
            this.renderer.hide(player, new MinionId(minionId));
        }
    }

    private boolean isVisible(Player player, Minion minion) {
        if (!player.getWorld().getKey().asString().equals(minion.position().worldKey())) {
            return false;
        }
        double distanceX = player.getX() - (minion.position().blockX() + 0.5D);
        double distanceY = player.getY() - minion.position().blockY();
        double distanceZ = player.getZ() - (minion.position().blockZ() + 0.5D);
        return distanceX * distanceX + distanceY * distanceY + distanceZ * distanceZ <= this.renderDistanceSquared;
    }
}
