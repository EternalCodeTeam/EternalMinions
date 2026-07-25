package com.eternalcode.minions.minion.crafter;

import java.util.Map;
import org.bukkit.Material;

public final class RecipeMatcher {

    public boolean canCraft(MinionRecipe recipe, Map<Material, Integer> available) {
        for (Map.Entry<Material, Integer> ingredient : recipe.ingredients().entrySet()) {
            Integer have = available.get(ingredient.getKey());
            if (have == null || have < ingredient.getValue()) {
                return false;
            }
        }
        return true;
    }
}
