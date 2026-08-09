package com.eternalcode.minions.minion.behavior;

import java.util.Set;
import org.bukkit.Material;

public final class MaterialFilter {

    private final Set<Material> allowedMaterials;
    private final Set<Material> blockedMaterials;

    public MaterialFilter(Set<Material> allowedMaterials, Set<Material> blockedMaterials) {
        if (allowedMaterials == null || blockedMaterials == null) {
            throw new IllegalArgumentException("Allowed and blocked materials are required");
        }

        this.allowedMaterials = Set.copyOf(allowedMaterials);
        this.blockedMaterials = Set.copyOf(blockedMaterials);
    }

    public boolean allows(Material material) {
        if (material == null) {
            return false;
        }
        if (this.blockedMaterials.contains(material)) {
            return false;
        }

        return this.allowedMaterials.isEmpty() || this.allowedMaterials.contains(material);
    }
}
