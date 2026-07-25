package com.eternalcode.minions.minion.killer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

// Minions render as packet-only bodies (EntityLib WrapperEntity), not real Bukkit entities, so
// vanilla Looting never applies on its own - there is no server-side attacker for the game to
// read the weapon's enchantments from. This listener tracks the Looting level of the last KILLER
// hit per entity and, on death, applies an approximate Looting bonus (a per-level chance of one
// extra copy of an existing natural drop) - not a byte-exact reproduction of vanilla's table.
public final class KillerLootingListener implements Listener {

    private static final long ENTRY_TTL_MILLIS = 30_000L;

    private record PendingLooting(int level, long expiresAtMillis) {
    }

    private final Map<UUID, PendingLooting> pending = new HashMap<>();

    public void trackHit(UUID entityId, int lootingLevel) {
        if (lootingLevel <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        this.pending.entrySet().removeIf(entry -> entry.getValue().expiresAtMillis() < now);
        this.pending.put(entityId, new PendingLooting(lootingLevel, now + ENTRY_TTL_MILLIS));
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        PendingLooting entry = this.pending.remove(event.getEntity().getUniqueId());
        if (entry == null || event.getDrops().isEmpty()) {
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        java.util.List<ItemStack> bonus = new ArrayList<>();
        for (ItemStack drop : new ArrayList<>(event.getDrops())) {
            for (int level = 0; level < entry.level(); level++) {
                if (random.nextDouble() < 0.5D) {
                    ItemStack extra = drop.clone();
                    extra.setAmount(1);
                    bonus.add(extra);
                }
            }
        }
        event.getDrops().addAll(bonus);
    }
}
