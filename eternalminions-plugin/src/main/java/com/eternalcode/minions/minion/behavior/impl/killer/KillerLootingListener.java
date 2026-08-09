package com.eternalcode.minions.minion.behavior.impl.killer;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

public final class KillerLootingListener implements Listener {

    private final Cache<UUID, Integer> pendingLooting =
            Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofSeconds(30))
                    .maximumSize(10_000)
                    .build();

    public void trackHit(
            UUID entityId,
            int lootingLevel
    ) {
        if (lootingLevel <= 0) {
            this.pendingLooting.invalidate(entityId);
            return;
        }

        this.pendingLooting.put(
                entityId,
                lootingLevel
        );
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Integer lootingLevel = this.pendingLooting.asMap().remove(
                event.getEntity().getUniqueId()
        );

        if (
                lootingLevel == null
                        || lootingLevel <= 0
                        || event.getDrops().isEmpty()
        ) {
            return;
        }

        ThreadLocalRandom random =
                ThreadLocalRandom.current();

        List<ItemStack> bonusDrops =
                new ArrayList<>();

        for (ItemStack drop : event.getDrops()) {
            if (
                    drop == null
                            || drop.getType().isAir()
                            || drop.getAmount() <= 0
            ) {
                continue;
            }

            int bonusAmount = random.nextInt(
                    lootingLevel + 1
            );

            if (bonusAmount <= 0) {
                continue;
            }

            ItemStack bonus = drop.clone();

            bonus.setAmount(
                    Math.min(
                            bonusAmount,
                            bonus.getMaxStackSize()
                    )
            );

            bonusDrops.add(bonus);
        }

        event.getDrops().addAll(bonusDrops);
    }
}