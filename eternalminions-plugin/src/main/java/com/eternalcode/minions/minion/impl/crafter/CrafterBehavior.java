package com.eternalcode.minions.minion.impl.crafter;

import com.eternalcode.minions.config.AbstractMinionConfig;
import com.eternalcode.minions.config.ConfigService;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionBehavior;
import com.eternalcode.minions.minion.MinionContext;
import com.eternalcode.minions.minion.MinionResult;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Container;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

public final class CrafterBehavior implements MinionBehavior {

    private final CrafterConfig config;
    private final RecipeMatcher matcher = new RecipeMatcher();

    public CrafterBehavior(CrafterConfig config) {
        this.config = config;
    }

    public static CrafterBehavior create(
            ConfigService configs,
            File directory
    ) {
        CrafterConfig config = configs.load(
                CrafterConfig.class,
                new File(directory, "crafter.yml")
        );

        return new CrafterBehavior(config);
    }

    private static String recipeId(Recipe recipe) {
        if (recipe instanceof ShapedRecipe shapedRecipe) {
            return shapedRecipe.getKey().toString();
        }

        if (recipe instanceof ShapelessRecipe shapelessRecipe) {
            return shapelessRecipe.getKey().toString();
        }

        return NamespacedKey.MINECRAFT
                + ":unknown";
    }

    @Override
    public String id() {
        return "crafter";
    }

    @Override
    public AbstractMinionConfig config() {
        return this.config;
    }

    @Override
    public MinionResult execute(MinionContext context) {
        Minion minion = context.minion();
        ItemStack selectedResult = minion.equipment().tool();

        if (
                selectedResult == null
                        || selectedResult.getType().isAir()
        ) {
            return MinionResult.idle(
                    minion,
                    CrafterStatuses.NO_RECIPE_SELECTED
            );
        }

        Container chest = context.linkedChest();

        if (chest == null) {
            return MinionResult.idle(
                    minion,
                    CrafterStatuses.NO_CHEST
            );
        }

        List<MinionRecipe> recipes = this.findRecipes(
                selectedResult
        );

        if (recipes.isEmpty()) {
            return MinionResult.idle(
                    minion,
                    CrafterStatuses.NO_RECIPE_SELECTED
            );
        }

        boolean hadIngredients = false;
        ItemStack[] contents = chest.getInventory().getContents();

        for (MinionRecipe recipe : recipes) {
            RecipeMatcher.CraftResult result =
                    this.matcher.craft(recipe, contents);

            if (
                    result.state()
                            == RecipeMatcher.CraftResult.State.MISSING_INGREDIENTS
            ) {
                continue;
            }

            hadIngredients = true;

            if (
                    result.state()
                            == RecipeMatcher.CraftResult.State.NO_SPACE
            ) {
                continue;
            }

            chest.getInventory().setContents(
                    result.contents()
            );

            Minion updated = minion.withProgress(
                    minion.progress().advanced(this.config)
            );

            return MinionResult.worked(
                    updated,
                    CrafterStatuses.CRAFTING
            );
        }

        if (hadIngredients) {
            return MinionResult.idle(
                    minion,
                    CrafterStatuses.NO_ROOM_FOR_RESULT
            );
        }

        return MinionResult.idle(
                minion,
                CrafterStatuses.NO_INGREDIENTS
        );
    }

    private List<MinionRecipe> findRecipes(
            ItemStack selectedResult
    ) {
        ItemStack lookup = selectedResult.clone();
        lookup.setAmount(1);

        List<MinionRecipe> recipes = new ArrayList<>();

        for (Recipe recipe : Bukkit.getRecipesFor(lookup)) {
            MinionRecipe mapped = this.mapRecipe(
                    recipe,
                    selectedResult
            );

            if (mapped != null) {
                recipes.add(mapped);
            }
        }

        return List.copyOf(recipes);
    }

    private MinionRecipe mapRecipe(
            Recipe recipe,
            ItemStack selectedResult
    ) {
        ItemStack result = recipe.getResult();

        if (
                result.getType() != selectedResult.getType()
                        || result.getType().isAir()
        ) {
            return null;
        }

        List<RecipeChoice> ingredients =
                this.ingredients(recipe);

        if (ingredients.isEmpty()) {
            return null;
        }

        return new MinionRecipe(
                recipeId(recipe),
                ingredients,
                result
        );
    }

    private List<RecipeChoice> ingredients(
            Recipe recipe
    ) {
        List<RecipeChoice> ingredients = new ArrayList<>();

        if (recipe instanceof ShapedRecipe shapedRecipe) {
            for (RecipeChoice choice : shapedRecipe.getChoiceMap().values()) {
                if (choice != null) {
                    ingredients.add(choice);
                }
            }

            return ingredients;
        }

        if (recipe instanceof ShapelessRecipe shapelessRecipe) {
            for (
                    RecipeChoice choice
                    : shapelessRecipe.getChoiceList()
            ) {
                if (choice != null) {
                    ingredients.add(choice);
                }
            }

            return ingredients;
        }

        return List.of();
    }
}