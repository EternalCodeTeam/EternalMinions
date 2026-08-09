package com.eternalcode.minions.minion.behavior.impl.crafter;

import java.util.List;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;

public record MinionRecipe(
        String id,
        List<RecipeChoice> ingredients,
        ItemStack result
) {

    public MinionRecipe {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Recipe id must not be blank");
        }

        if (ingredients == null || ingredients.isEmpty()) {
            throw new IllegalArgumentException(
                    "Recipe " + id + " requires at least one ingredient"
            );
        }

        if (result == null || result.getType().isAir() || result.getAmount() < 1) {
            throw new IllegalArgumentException(
                    "Recipe " + id + " requires a valid result"
            );
        }

        ingredients = List.copyOf(ingredients);
        result = result.clone();
    }
}