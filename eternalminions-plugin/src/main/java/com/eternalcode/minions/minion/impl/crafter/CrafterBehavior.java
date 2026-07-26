package com.eternalcode.minions.minion.impl.crafter;

import com.cryptomorin.xseries.XMaterial;
import com.eternalcode.minions.config.AbstractMinionConfig;
import com.eternalcode.minions.config.MinionRecipeConfig;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionBehavior;
import com.eternalcode.minions.minion.MinionContext;
import com.eternalcode.minions.minion.MinionResult;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.block.Container;
import org.bukkit.inventory.ItemStack;

public final class CrafterBehavior implements MinionBehavior {

    private final CrafterConfig config;
    private final RecipeMatcher matcher = new RecipeMatcher();
    private final List<MinionRecipe> recipes;

    public CrafterBehavior(CrafterConfig config) {
        this.config = config;
        this.recipes = mapRecipes(config.recipes);
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
        if (selectedResult == null || selectedResult.getType().isAir()) {
            return MinionResult.idle(minion, CrafterStatuses.NO_RECIPE_SELECTED);
        }

        MinionRecipe recipe = this.findRecipe(selectedResult.getType());
        if (recipe == null) {
            return MinionResult.idle(minion, CrafterStatuses.NO_RECIPE_SELECTED);
        }
        Container chest = context.linkedChest();
        if (chest == null) {
            return MinionResult.idle(minion, CrafterStatuses.NO_CHEST);
        }
        if (!this.matcher.canCraft(recipe, this.countAvailable(chest, recipe))) {
            return MinionResult.idle(minion, CrafterStatuses.NO_INGREDIENTS);
        }

        ItemStack craftedItem = new ItemStack(recipe.resultMaterial(), recipe.resultAmount());
        Map<Integer, ItemStack> leftover = chest.getInventory().addItem(craftedItem);
        if (!leftover.isEmpty()) {
            return MinionResult.idle(minion, CrafterStatuses.NO_ROOM_FOR_RESULT);
        }

        this.removeIngredients(chest, recipe);
        Minion updated = minion.withProgress(minion.progress().advanced(this.config));
        return MinionResult.worked(updated, CrafterStatuses.CRAFTING);
    }

    private MinionRecipe findRecipe(Material resultMaterial) {
        for (MinionRecipe recipe : this.recipes) {
            if (recipe.resultMaterial() == resultMaterial) {
                return recipe;
            }
        }
        return null;
    }

    private Map<Material, Integer> countAvailable(Container chest, MinionRecipe recipe) {
        Map<Material, Integer> counts = new EnumMap<>(Material.class);
        for (ItemStack item : chest.getInventory().getContents()) {
            if (item != null && recipe.ingredients().containsKey(item.getType())) {
                counts.merge(item.getType(), item.getAmount(), Integer::sum);
            }
        }
        return counts;
    }

    private void removeIngredients(Container chest, MinionRecipe recipe) {
        Map<Material, Integer> remaining = new EnumMap<>(recipe.ingredients());
        ItemStack[] contents = chest.getInventory().getContents();
        for (int slot = 0; slot < contents.length && !remaining.isEmpty(); slot++) {
            ItemStack item = contents[slot];
            if (item == null) {
                continue;
            }
            Integer needed = remaining.get(item.getType());
            if (needed == null) {
                continue;
            }

            int takenAmount = Math.min(needed, item.getAmount());
            int remainingInSlot = item.getAmount() - takenAmount;
            chest.getInventory().setItem(slot, remainingInSlot <= 0 ? null : withAmount(item, remainingInSlot));
            int stillNeeded = needed - takenAmount;
            if (stillNeeded <= 0) {
                remaining.remove(item.getType());
            }
            else {
                remaining.put(item.getType(), stillNeeded);
            }
        }
    }

    private static List<MinionRecipe> mapRecipes(List<MinionRecipeConfig> configuredRecipes) {
        List<MinionRecipe> recipes = new ArrayList<>();
        for (MinionRecipeConfig configuredRecipe : configuredRecipes) {
            Map<Material, Integer> ingredients = new EnumMap<>(Material.class);
            for (Map.Entry<XMaterial, Integer> entry : configuredRecipe.ingredients.entrySet()) {
                Material material = entry.getKey().parseMaterial();
                if (material != null) {
                    ingredients.put(material, entry.getValue());
                }
            }
            Material resultMaterial = configuredRecipe.resultMaterial.parseMaterial();
            if (resultMaterial == null || ingredients.isEmpty()) {
                continue;
            }
            recipes.add(new MinionRecipe(
                configuredRecipe.id,
                configuredRecipe.displayName,
                ingredients,
                resultMaterial,
                configuredRecipe.resultAmount
            ));
        }
        return List.copyOf(recipes);
    }

    private static ItemStack withAmount(ItemStack item, int amount) {
        ItemStack copy = item.clone();
        copy.setAmount(amount);
        return copy;
    }

}
