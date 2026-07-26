package com.eternalcode.minions.minion.crafter;

import static org.assertj.core.api.Assertions.assertThat;

import com.eternalcode.minions.minion.impl.crafter.MinionRecipe;
import com.eternalcode.minions.minion.impl.crafter.RecipeMatcher;
import java.util.EnumMap;
import java.util.Map;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class RecipeMatcherTest {

    private final RecipeMatcher matcher = new RecipeMatcher();

    private MinionRecipe recipe() {
        Map<Material, Integer> ingredients = new EnumMap<>(Material.class);
        ingredients.put(Material.OAK_PLANKS, 2);
        ingredients.put(Material.STRING, 1);
        return new MinionRecipe("sticks", "Sticks", ingredients, Material.STICK, 4);
    }

    @Test
    void canCraftWhenEveryIngredientMeetsItsAmount() {
        Map<Material, Integer> available = Map.of(Material.OAK_PLANKS, 2, Material.STRING, 1);
        assertThat(this.matcher.canCraft(this.recipe(), available)).isTrue();
    }

    @Test
    void cannotCraftWhenOneIngredientIsMissing() {
        Map<Material, Integer> available = Map.of(Material.OAK_PLANKS, 2);
        assertThat(this.matcher.canCraft(this.recipe(), available)).isFalse();
    }

    @Test
    void cannotCraftWhenAnIngredientAmountIsTooLow() {
        Map<Material, Integer> available = Map.of(Material.OAK_PLANKS, 1, Material.STRING, 1);
        assertThat(this.matcher.canCraft(this.recipe(), available)).isFalse();
    }
}
