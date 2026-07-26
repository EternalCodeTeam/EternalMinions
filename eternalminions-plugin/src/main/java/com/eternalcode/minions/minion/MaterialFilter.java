package com.eternalcode.minions.minion;

import java.util.Set;
import org.bukkit.Material;

public final class MaterialFilter {

    private final Set<Material> allowed;
    private final Set<Material> blocked;

    public MaterialFilter(Set<Material> allowed, Set<Material> blocked) {
        if (allowed == null || blocked == null) {
            throw new IllegalArgumentException("Allowed and blocked materials are required");
        }

        this.allowed = Set.copyOf(allowed);
        this.blocked = Set.copyOf(blocked);
    }

    public boolean allows(Material material) {
        if (material == null) {
            return false;
        }
        if (this.blocked.contains(material)) {
            return false;
        }

        return this.allowed.isEmpty() || this.allowed.contains(material);
    }
}
