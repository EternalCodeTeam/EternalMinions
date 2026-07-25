package com.eternalcode.minions.minion;

import com.eternalcode.minions.minion.crafter.CrafterWork;
import com.eternalcode.minions.minion.farmer.FarmerWork;
import com.eternalcode.minions.minion.fisherman.FisherWork;
import com.eternalcode.minions.minion.killer.KillerWork;
import com.eternalcode.minions.minion.lumberjack.LumberjackWork;
import com.eternalcode.minions.minion.tool.ToolRequirement;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;

// Behavior-specific tuning grouped in one value so MinionType stays constructable without a huge argument list.
public final class MinionWork {

    private final ToolRequirement toolRequirement;
    private final int collectorRadius;
    private final Set<Material> collectorAllowedMaterials;
    private final Set<Material> collectorBlockedMaterials;
    private final Map<Material, Double> sellPrices;
    private final int sellBatch;
    private final LumberjackWork lumberjack;
    private final FarmerWork farmer;
    private final FisherWork fisher;
    private final KillerWork killer;
    private final CrafterWork crafter;

    public MinionWork(
        ToolRequirement toolRequirement,
        int collectorRadius,
        Set<Material> collectorAllowedMaterials,
        Set<Material> collectorBlockedMaterials,
        Map<Material, Double> sellPrices,
        int sellBatch,
        LumberjackWork lumberjack,
        FarmerWork farmer,
        FisherWork fisher,
        KillerWork killer,
        CrafterWork crafter
    ) {
        if (toolRequirement == null || sellPrices == null) {
            throw new IllegalArgumentException("Minion work requires a tool requirement and sell prices");
        }
        if (collectorRadius < 1 || collectorRadius > 16) {
            throw new IllegalArgumentException("Collector radius must be between 1 and 16");
        }
        if (collectorAllowedMaterials == null || collectorBlockedMaterials == null) {
            throw new IllegalArgumentException("Minion work requires collector filter sets");
        }
        if (sellBatch < 1) {
            throw new IllegalArgumentException("Sell batch must be positive");
        }
        if (lumberjack == null || farmer == null || fisher == null || killer == null || crafter == null) {
            throw new IllegalArgumentException(
                "Minion work requires every per-profession config, even for types that do not use it");
        }
        this.toolRequirement = toolRequirement;
        this.collectorRadius = collectorRadius;
        this.collectorAllowedMaterials = collectorAllowedMaterials.isEmpty()
            ? Set.of() : EnumSet.copyOf(collectorAllowedMaterials);
        this.collectorBlockedMaterials = collectorBlockedMaterials.isEmpty()
            ? Set.of() : EnumSet.copyOf(collectorBlockedMaterials);
        this.sellPrices = sellPrices.isEmpty() ? Map.of() : new EnumMap<>(sellPrices);
        this.sellBatch = sellBatch;
        this.lumberjack = lumberjack;
        this.farmer = farmer;
        this.fisher = fisher;
        this.killer = killer;
        this.crafter = crafter;
    }

    public boolean collectorAllows(Material material) {
        if (this.collectorBlockedMaterials.contains(material)) {
            return false;
        }
        return this.collectorAllowedMaterials.isEmpty() || this.collectorAllowedMaterials.contains(material);
    }

    public ToolRequirement toolRequirement() {
        return this.toolRequirement;
    }

    public int collectorRadius() {
        return this.collectorRadius;
    }

    public int sellBatch() {
        return this.sellBatch;
    }

    public Set<Material> sellableMaterials() {
        return this.sellPrices.keySet();
    }

    public double sellPrice(Material material) {
        Double price = this.sellPrices.get(material);
        return price == null ? 0.0D : price;
    }

    public LumberjackWork lumberjack() {
        return this.lumberjack;
    }

    public FarmerWork farmer() {
        return this.farmer;
    }

    public FisherWork fisher() {
        return this.fisher;
    }

    public KillerWork killer() {
        return this.killer;
    }

    public CrafterWork crafter() {
        return this.crafter;
    }
}
