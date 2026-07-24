package com.eternalcode.minions.minion;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;

// Behavior-specific tuning grouped in one value so MinionType stays constructable without a huge argument list.
public final class MinionWork {

    private final MinionDrop[] drops;
    private final boolean requiresWater;
    private final int collectorRadius;
    private final Map<Material, Double> sellPrices;
    private final int sellBatch;

    public MinionWork(
        List<MinionDrop> drops,
        boolean requiresWater,
        int collectorRadius,
        Map<Material, Double> sellPrices,
        int sellBatch
    ) {
        if (drops == null || sellPrices == null) {
            throw new IllegalArgumentException("Minion work requires drops and sell prices");
        }
        if (collectorRadius < 1 || collectorRadius > 16) {
            throw new IllegalArgumentException("Collector radius must be between 1 and 16");
        }
        if (sellBatch < 1) {
            throw new IllegalArgumentException("Sell batch must be positive");
        }
        this.drops = drops.toArray(new MinionDrop[0]);
        this.requiresWater = requiresWater;
        this.collectorRadius = collectorRadius;
        this.sellPrices = sellPrices.isEmpty() ? Map.of() : new EnumMap<>(sellPrices);
        this.sellBatch = sellBatch;
    }

    public MinionDrop[] drops() {
        return this.drops;
    }

    public boolean requiresWater() {
        return this.requiresWater;
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
}
