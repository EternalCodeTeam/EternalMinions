package com.eternalcode.minions.minion.farmer;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;

// FARMER-type tuning: which crop blocks it recognizes, and which seed item each one needs back to
// be replanted after harvest (wheat -> wheat seeds, etc). Sugar cane/bamboo/pumpkin/melon are
// handled structurally by FarmerBehavior, not through this map, since they never go bare.
public record FarmerWork(Set<Material> cropMaterials, Map<Material, Material> seedByCrop) {

    public FarmerWork {
        if (cropMaterials == null || cropMaterials.isEmpty()) {
            throw new IllegalArgumentException("Farmer work requires at least one crop material");
        }
        if (seedByCrop == null) {
            throw new IllegalArgumentException("Farmer work requires a seed-by-crop map");
        }
        cropMaterials = Set.copyOf(cropMaterials);
        seedByCrop = seedByCrop.isEmpty() ? Map.of() : new EnumMap<>(seedByCrop);
    }
}
