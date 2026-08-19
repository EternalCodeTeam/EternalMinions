package com.eternalcode.minions.minion.activity;

import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.activity.config.ActivityBypassConfig;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.UUID;
import org.bukkit.entity.Player;

public final class MinionActivityBypass {

    private static final Duration CACHE_TTL = Duration.ofSeconds(5);

    private final ActivityBypassConfig config;
    private final Cache<UUID, Boolean> permissionCache = Caffeine.newBuilder()
            .expireAfterWrite(CACHE_TTL)
            .build();

    public MinionActivityBypass(ActivityBypassConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Activity bypass config is required");
        }
        this.config = config;
    }

    public boolean isBypassed(Minion minion, Player owner) {
        if (this.config.exemptMinionTypes.contains(minion.behaviorId())) {
            return true;
        }
        if (owner == null || this.config.permission.isBlank()) {
            return false;
        }
        return this.permissionCache.get(owner.getUniqueId(), ignored -> owner.hasPermission(this.config.permission));
    }
}
