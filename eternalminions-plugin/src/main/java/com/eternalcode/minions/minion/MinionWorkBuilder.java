package com.eternalcode.minions.minion;

import com.eternalcode.minions.minion.crafter.CrafterWork;
import com.eternalcode.minions.minion.farmer.FarmerWork;
import com.eternalcode.minions.minion.fisherman.FisherWork;
import com.eternalcode.minions.minion.killer.KillerWork;
import com.eternalcode.minions.minion.lumberjack.LumberjackWork;
import com.eternalcode.minions.minion.tool.ToolRequirement;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

// Builds a MinionWork, filling any profession's tuning that the caller does not set with a
// harmless default - every MinionType needs a valid MinionWork even for professions it never uses.
final class MinionWorkBuilder {

    private final ToolRequirement toolRequirement;
    private int collectorRadius = 4;
    private Set<Material> collectorAllowed = Set.of();
    private Set<Material> collectorBlocked = Set.of();
    private Map<Material, Double> sellPrices = Map.of();
    private int sellBatch = 64;
    private LumberjackWork lumberjack = new LumberjackWork(Material.OAK_LOG, Material.OAK_SAPLING, 128);
    private FarmerWork farmer = new FarmerWork(EnumSet.of(Material.WHEAT), Map.of());
    private FisherWork fisher = new FisherWork(1, 100, 0);
    private KillerWork killer = new KillerWork(EnumSet.of(EntityType.ZOMBIE), 4, 20, 1.0);
    private CrafterWork crafter = new CrafterWork(List.of());

    MinionWorkBuilder(ToolRequirement toolRequirement) {
        this.toolRequirement = toolRequirement;
    }

    MinionWorkBuilder collectorRadius(int collectorRadius) {
        this.collectorRadius = collectorRadius;
        return this;
    }

    MinionWorkBuilder collectorAllowed(Set<Material> collectorAllowed) {
        this.collectorAllowed = collectorAllowed;
        return this;
    }

    MinionWorkBuilder collectorBlocked(Set<Material> collectorBlocked) {
        this.collectorBlocked = collectorBlocked;
        return this;
    }

    MinionWorkBuilder sellPrices(Map<Material, Double> sellPrices) {
        this.sellPrices = sellPrices;
        return this;
    }

    MinionWorkBuilder sellBatch(int sellBatch) {
        this.sellBatch = sellBatch;
        return this;
    }

    MinionWorkBuilder lumberjack(LumberjackWork lumberjack) {
        this.lumberjack = lumberjack;
        return this;
    }

    MinionWorkBuilder farmer(FarmerWork farmer) {
        this.farmer = farmer;
        return this;
    }

    MinionWorkBuilder fisher(FisherWork fisher) {
        this.fisher = fisher;
        return this;
    }

    MinionWorkBuilder killer(KillerWork killer) {
        this.killer = killer;
        return this;
    }

    MinionWorkBuilder crafter(CrafterWork crafter) {
        this.crafter = crafter;
        return this;
    }

    MinionWork build() {
        return new MinionWork(
            this.toolRequirement,
            this.collectorRadius,
            this.collectorAllowed,
            this.collectorBlocked,
            this.sellPrices,
            this.sellBatch,
            this.lumberjack,
            this.farmer,
            this.fisher,
            this.killer,
            this.crafter
        );
    }
}
