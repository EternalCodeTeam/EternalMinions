package com.eternalcode.minions.minion.impl.crafter;

import java.util.EnumMap;
import java.util.Map;
import org.bukkit.Material;

// A single owner-selectable recipe: ingredient materials/amounts consumed from storage+chest, and
// the resulting material/amount produced. No NBT/custom-item results, matching the existing
// MinionDrop convention of plain Material+amount, so recipes stay expressible in YAML.
public record MinionRecipe(String id, String displayName, Map<Material, Integer> ingredients, Material resultMaterial, int resultAmount) {

    public MinionRecipe {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Minion recipe id must not be blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Minion recipe " + id + " requires a display name");
        }
        if (ingredients == null || ingredients.isEmpty()) {
            throw new IllegalArgumentException("Minion recipe " + id + " requires at least one ingredient");
        }
        if (resultMaterial == null || resultAmount < 1) {
            throw new IllegalArgumentException("Minion recipe " + id + " requires a positive result amount");
        }
        for (Integer amount : ingredients.values()) {
            if (amount == null || amount < 1) {
                throw new IllegalArgumentException("Minion recipe " + id + " ingredient amounts must be positive");
            }
        }
        ingredients = new EnumMap<>(ingredients);
    }
}
