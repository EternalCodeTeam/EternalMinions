package com.eternalcode.minions.minion.impl.crafter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;

public final class RecipeMatcher {

    public CraftResult craft(
            MinionRecipe recipe,
            ItemStack[] inventoryContents
    ) {
        ItemStack[] contents = cloneContents(inventoryContents);
        List<Ingredient> ingredients = this.ingredients(
                recipe,
                contents
        );

        ingredients.sort(
                Comparator.comparingInt(Ingredient::matchingSlots)
        );

        if (!this.consumeIngredients(ingredients, 0, contents)) {
            return CraftResult.missingIngredients();
        }

        if (!addItem(contents, recipe.result())) {
            return CraftResult.noSpace();
        }

        return CraftResult.success(contents);
    }

    private boolean consumeIngredients(
            List<Ingredient> ingredients,
            int ingredientIndex,
            ItemStack[] contents
    ) {
        if (ingredientIndex >= ingredients.size()) {
            return true;
        }

        Ingredient ingredient = ingredients.get(ingredientIndex);

        for (int slot : ingredient.slots()) {
            ItemStack item = contents[slot];

            if (item == null || item.getType().isAir()) {
                continue;
            }

            if (!ingredient.choice().test(item)) {
                continue;
            }

            ItemStack previous = item.clone();

            if (item.getAmount() <= 1) {
                contents[slot] = null;
            }
            else {
                item.setAmount(item.getAmount() - 1);
            }

            if (this.consumeIngredients(
                    ingredients,
                    ingredientIndex + 1,
                    contents
            )) {
                return true;
            }

            contents[slot] = previous;
        }

        return false;
    }

    private List<Ingredient> ingredients(
            MinionRecipe recipe,
            ItemStack[] contents
    ) {
        List<Ingredient> ingredients = new ArrayList<>(
                recipe.ingredients().size()
        );

        for (RecipeChoice choice : recipe.ingredients()) {
            List<Integer> slots = new ArrayList<>();

            for (int slot = 0; slot < contents.length; slot++) {
                ItemStack item = contents[slot];

                if (item == null || item.getType().isAir()) {
                    continue;
                }

                if (choice.test(item)) {
                    slots.add(slot);
                }
            }

            ingredients.add(
                    new Ingredient(choice, List.copyOf(slots))
            );
        }

        return ingredients;
    }

    private static boolean addItem(
            ItemStack[] contents,
            ItemStack item
    ) {
        ItemStack remaining = item.clone();

        for (ItemStack current : contents) {
            if (current == null || current.getType().isAir()) {
                continue;
            }

            if (!current.isSimilar(remaining)) {
                continue;
            }

            int maximumSize = Math.min(
                    current.getMaxStackSize(),
                    remaining.getMaxStackSize()
            );

            int room = maximumSize - current.getAmount();

            if (room <= 0) {
                continue;
            }

            int added = Math.min(room, remaining.getAmount());

            current.setAmount(current.getAmount() + added);
            remaining.setAmount(remaining.getAmount() - added);

            if (remaining.getAmount() <= 0) {
                return true;
            }
        }

        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack current = contents[slot];

            if (current != null && !current.getType().isAir()) {
                continue;
            }

            int added = Math.min(
                    remaining.getAmount(),
                    remaining.getMaxStackSize()
            );

            ItemStack inserted = remaining.clone();
            inserted.setAmount(added);

            contents[slot] = inserted;
            remaining.setAmount(remaining.getAmount() - added);

            if (remaining.getAmount() <= 0) {
                return true;
            }
        }

        return false;
    }

    private static ItemStack[] cloneContents(
            ItemStack[] contents
    ) {
        ItemStack[] cloned = new ItemStack[contents.length];

        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];

            if (item != null) {
                cloned[slot] = item.clone();
            }
        }

        return cloned;
    }

    private record Ingredient(
            RecipeChoice choice,
            List<Integer> slots
    ) {

        private int matchingSlots() {
            return this.slots.size();
        }
    }

    public record CraftResult(
            State state,
            ItemStack[] contents
    ) {

        public static CraftResult success(ItemStack[] contents) {
            return new CraftResult(
                    State.SUCCESS,
                    contents
            );
        }

        public static CraftResult missingIngredients() {
            return new CraftResult(
                    State.MISSING_INGREDIENTS,
                    null
            );
        }

        public static CraftResult noSpace() {
            return new CraftResult(
                    State.NO_SPACE,
                    null
            );
        }

        public enum State {

            SUCCESS,
            MISSING_INGREDIENTS,
            NO_SPACE
        }
    }
}