package com.eternalcode.minions.minion;

import com.eternalcode.minions.config.ConfigService;
import com.eternalcode.minions.minion.impl.collector.CollectorBehavior;
import com.eternalcode.minions.minion.impl.crafter.CrafterBehavior;
import com.eternalcode.minions.minion.impl.farmer.FarmerBehavior;
import com.eternalcode.minions.minion.impl.fisherman.FishermanBehavior;
import com.eternalcode.minions.minion.impl.killer.KillerBehavior;
import com.eternalcode.minions.minion.impl.killer.KillerLootingListener;
import com.eternalcode.minions.minion.impl.lumberjack.LumberjackBehavior;
import com.eternalcode.minions.minion.impl.miner.MiningBehavior;
import com.eternalcode.minions.minion.impl.seller.SellerBehavior;
import com.eternalcode.minions.minion.storage.MinionItemTransferService;
import com.eternalcode.minions.minion.tool.MinionToolService;
import com.eternalcode.minions.shop.MinionShopProvider;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class MinionBehaviorRegistry {

    private volatile Map<String, MinionBehavior> behaviors = Map.of();

    public static List<MinionBehavior> createEnabled(
        ConfigService configs,
        File directory,
        MinionToolService tools,
        MinionItemTransferService transfers,
        KillerLootingListener killerLooting,
        MinionShopProvider shop
    ) {
        List<MinionBehavior> all = List.of(
            MiningBehavior.create(configs, directory, tools, transfers),
            LumberjackBehavior.create(configs, directory, tools, transfers),
            FarmerBehavior.create(configs, directory, tools, transfers),
            FishermanBehavior.create(configs, directory, tools, transfers),
            KillerBehavior.create(configs, directory, tools, killerLooting),
            CollectorBehavior.create(configs, directory, tools, transfers),
            CrafterBehavior.create(configs, directory, transfers),
            SellerBehavior.create(configs, directory, shop)
        );

        return all.stream()
            .filter(behavior -> behavior.config().enabled)
            .toList();
    }

    public MinionBehavior require(String behaviorId) {
        MinionBehavior behavior = this.find(behaviorId).orElse(null);
        if (behavior == null) {
            throw new IllegalArgumentException("Unknown minion behavior: " + behaviorId);
        }
        return behavior;
    }

    public Optional<MinionBehavior> find(String behaviorId) {
        if (behaviorId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.behaviors.get(normalize(behaviorId)));
    }

    public MinionBehavior defaultBehavior() {
        if (this.behaviors.isEmpty()) {
            throw new IllegalStateException("No minion behaviors are registered");
        }
        return this.behaviors.values().iterator().next();
    }

    public Collection<MinionBehavior> behaviors() {
        return this.behaviors.values();
    }

    public List<String> ids() {
        return List.copyOf(this.behaviors.keySet());
    }

    public void replace(Collection<MinionBehavior> behaviors) {
        if (behaviors == null || behaviors.isEmpty()) {
            throw new IllegalArgumentException("At least one minion behavior is required");
        }

        Map<String, MinionBehavior> rebuilt = new LinkedHashMap<>();
        for (MinionBehavior behavior : behaviors) {
            if (behavior == null) {
                throw new IllegalArgumentException("Minion behavior must not be null");
            }

            String behaviorId = normalize(behavior.id());
            if (rebuilt.putIfAbsent(behaviorId, behavior) != null) {
                throw new IllegalArgumentException("Duplicate minion behavior: " + behaviorId);
            }
        }
        this.behaviors = Collections.unmodifiableMap(rebuilt);
    }

    private static String normalize(String behaviorId) {
        if (behaviorId == null || behaviorId.isBlank()) {
            throw new IllegalArgumentException("Minion behavior id must not be blank");
        }
        return behaviorId.toLowerCase(Locale.ROOT);
    }
}
