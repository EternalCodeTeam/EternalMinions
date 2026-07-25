package com.eternalcode.minions.minion.crafter;

import java.util.List;

// All recipes an owner can pick between for one CRAFTER-type minion.
public record CrafterWork(List<MinionRecipe> recipes) {

    public CrafterWork {
        if (recipes == null) {
            throw new IllegalArgumentException("Crafter work requires a recipe list");
        }
        recipes = List.copyOf(recipes);
    }
}
