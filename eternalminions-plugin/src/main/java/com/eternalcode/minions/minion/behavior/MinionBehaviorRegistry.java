package com.eternalcode.minions.minion.behavior;

import com.eternalcode.minions.config.ConfigService;
import com.eternalcode.minions.minion.behavior.impl.collector.CollectorBehavior;
import com.eternalcode.minions.minion.behavior.impl.crafter.CrafterBehavior;
import com.eternalcode.minions.minion.behavior.impl.farmer.FarmerBehavior;
import com.eternalcode.minions.minion.behavior.impl.fisherman.FishermanBehavior;
import com.eternalcode.minions.minion.behavior.impl.killer.KillerBehavior;
import com.eternalcode.minions.minion.behavior.impl.killer.KillerLootingListener;
import com.eternalcode.minions.minion.behavior.impl.lumberjack.LumberjackBehavior;
import com.eternalcode.minions.minion.behavior.impl.miner.MiningBehavior;
import com.eternalcode.minions.minion.behavior.impl.seller.SellerBehavior;
import com.eternalcode.minions.minion.storage.MinionItemTransferService;
import com.eternalcode.minions.minion.tool.MinionToolService;
import com.eternalcode.minions.shop.MinionShopProvider;
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
        MinionToolService tools,
        MinionItemTransferService transfers,
        KillerLootingListener killerLooting,
        MinionShopProvider shop
    ) {
        List<MinionBehavior> all = List.of(
            MiningBehavior.create(configs, tools, transfers),
            LumberjackBehavior.create(configs, tools, transfers),
            FarmerBehavior.create(configs, tools, transfers),
            FishermanBehavior.create(configs, tools, transfers),
            KillerBehavior.create(configs, tools, killerLooting),
            CollectorBehavior.create(configs, tools, transfers),
            CrafterBehavior.create(configs, transfers),
            SellerBehavior.create(configs, shop)
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
