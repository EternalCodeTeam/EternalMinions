package com.eternalcode.minions.minion.impl.crafter;

import com.eternalcode.minions.config.AbstractMinionConfig;
import com.eternalcode.minions.config.ConfigService;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionBehavior;
import com.eternalcode.minions.minion.MinionContext;
import com.eternalcode.minions.minion.MinionResult;
import com.eternalcode.minions.minion.WorkLimit;
import com.eternalcode.minions.minion.storage.MinionItemTransferService;
import com.eternalcode.minions.minion.storage.MinionStorage;
import com.eternalcode.minions.minion.storage.MinionStorageUpdate;
import com.eternalcode.minions.minion.status.CoreMinionStatuses;
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
    private final MinionItemTransferService transfers;
    private final RecipeMatcher matcher = new RecipeMatcher();

    public CrafterBehavior(CrafterConfig config, MinionItemTransferService transfers) {
        if (config == null || transfers == null) {
            throw new IllegalArgumentException("Crafter config and item transfers are required");
        }
        this.config = config;
        this.transfers = transfers;
        WorkLimit.validate("crafter.maxCraftsPerCycle", config.maxCraftsPerCycle);
        if (config.maximumCraftsSafetyCap < 1) {
            throw new IllegalArgumentException(
                    "crafter.maximumCraftsSafetyCap must be positive: "
                            + config.maximumCraftsSafetyCap
            );
        }
    }

    public static CrafterBehavior create(
            ConfigService configs,
            File directory,
            MinionItemTransferService transfers
    ) {
        CrafterConfig config = configs.load(
                CrafterConfig.class,
                new File(directory, "crafter.yml")
        );

        return new CrafterBehavior(config, transfers);
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

        ItemStack[] contents = chest.getInventory().getContents();
        int craftLimit = this.config.maxCraftsPerCycle == 0
                ? this.config.maximumCraftsSafetyCap
                : Math.min(this.config.maxCraftsPerCycle, this.config.maximumCraftsSafetyCap);
        int craftedItems = 0;
        boolean hadIngredients = false;
        MinionStorage storage = minion.storage();

        while (craftedItems < craftLimit) {
            RecipeMatcher.CraftResult craft = this.findCraft(recipes, contents);
            if (craft == null) {
                break;
            }
            if (craft.state() == RecipeMatcher.CraftResult.State.NO_SPACE) {
                hadIngredients = true;
                MinionStorageUpdate storedResult = storage.add(craft.overflow());
                ItemStack overflow = storedResult.remaining();
                if (overflow != null && !this.transfers.dropsOverflowItems()) {
                    break;
                }
                contents = craft.contents();
                storage = storedResult.storage();
                if (overflow != null) {
                    context.world().dropItemNaturally(context.location(), overflow);
                }
                craftedItems++;
                continue;
            }
            if (craft.state() == RecipeMatcher.CraftResult.State.MISSING_INGREDIENTS) {
                break;
            }
            contents = craft.contents();
            craftedItems++;
        }

        if (craftedItems > 0) {
            chest.getInventory().setContents(contents);
            Minion updated = minion.withStorage(storage);
            for (int craftedItem = 0; craftedItem < craftedItems; craftedItem++) {
                updated = updated.withProgress(updated.progress().advanced(this.config));
            }
            return MinionResult.worked(updated, CrafterStatuses.CRAFTING);
        }
        if (hadIngredients) {
            return MinionResult.idle(
                    minion,
                    CoreMinionStatuses.STORAGE_FULL
            );
        }

        return MinionResult.idle(
                minion,
                CrafterStatuses.NO_INGREDIENTS
        );
    }

    private RecipeMatcher.CraftResult findCraft(
            List<MinionRecipe> recipes,
            ItemStack[] contents
    ) {
        RecipeMatcher.CraftResult blockedCraft = null;
        for (MinionRecipe recipe : recipes) {
            RecipeMatcher.CraftResult craft = this.matcher.craft(recipe, contents);
            if (craft.state() == RecipeMatcher.CraftResult.State.SUCCESS) {
                return craft;
            }
            if (craft.state() == RecipeMatcher.CraftResult.State.NO_SPACE) {
                blockedCraft = craft;
            }
        }
        return blockedCraft;
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
