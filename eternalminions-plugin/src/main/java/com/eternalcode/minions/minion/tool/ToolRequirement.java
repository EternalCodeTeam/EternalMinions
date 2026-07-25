package com.eternalcode.minions.minion.tool;

import java.util.Set;
import org.bukkit.Material;

public record ToolRequirement(
    ToolCategory category,
    Set<Material> allowedMaterials,
    int minDurabilityToKeep,
    boolean required
) {

    public ToolRequirement {
        if (category == null) {
            throw new IllegalArgumentException("Tool requirement category must not be null");
        }
        if (allowedMaterials == null) {
            throw new IllegalArgumentException("Tool requirement allowed materials must not be null");
        }
        if (minDurabilityToKeep < 1) {
            throw new IllegalArgumentException("Tool requirement minimum durability to keep must be positive");
        }

        allowedMaterials = Set.copyOf(allowedMaterials);
    }

    public boolean matches(Material material) {
        if (material == null) {
            throw new IllegalArgumentException("Tool material must not be null");
        }
        if (this.category == ToolCategory.NONE || material == Material.AIR) {
            return false;
        }
        if (!this.allowedMaterials.isEmpty()) {
            return this.allowedMaterials.contains(material);
        }
        if (this.category == ToolCategory.ANY) {
            return true;
        }

        return ToolCategory.of(material) == this.category;
    }
}
