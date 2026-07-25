package com.eternalcode.minions.minion.crafter;

import com.eternalcode.minions.database.MinionPersistenceService;
import com.eternalcode.minions.minion.AbstractMinionBehavior;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionRegistry;
import com.eternalcode.minions.minion.MinionType;
import com.eternalcode.minions.minion.ScheduledMinion;
import com.eternalcode.minions.minion.status.MinionStatusTracker;
import com.eternalcode.minions.render.MinionRenderer;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Container;
import org.bukkit.inventory.ItemStack;

// The tool slot holds an example of the item to craft; the recipe is matched by its result
// material. Ingredients are pulled from the linked chest only and the crafted result is deposited
// back into that same chest only - no minion storage involvement.
public final class CrafterBehavior extends AbstractMinionBehavior {

    private final RecipeMatcher matcher = new RecipeMatcher();

    public CrafterBehavior(
        MinionRegistry registry,
        MinionPersistenceService persistence,
        MinionRenderer renderer,
        MinionStatusTracker statuses
    ) {
        super(registry, persistence, renderer, statuses);
    }

    @Override
    public boolean execute(Minion minion, MinionType type, ScheduledMinion scheduledMinion, World world) {
        ItemStack toolItem = minion.equipment().tool();
        if (toolItem == null || toolItem.getType().isAir()) {
            this.refreshStatusIfChanged(minion, CrafterStatuses.NO_RECIPE_SELECTED);
            return false;
        }

        List<MinionRecipe> recipes = type.work().crafter().recipes();
        MinionRecipe recipe = null;
        for (MinionRecipe candidate : recipes) {
            if (candidate.resultMaterial() == toolItem.getType()) {
                recipe = candidate;
                break;
            }
        }
        if (recipe == null) {
            this.refreshStatusIfChanged(minion, CrafterStatuses.NO_RECIPE_SELECTED);
            return false;
        }

        Container chest = this.resolveLinkedChest(minion, world);
        if (chest == null) {
            this.refreshStatusIfChanged(minion, CrafterStatuses.NO_CHEST);
            return false;
        }

        Map<Material, Integer> available = this.countAvailable(chest, recipe);
        if (!this.matcher.canCraft(recipe, available)) {
            this.refreshStatusIfChanged(minion, CrafterStatuses.NO_INGREDIENTS);
            return false;
        }

        ItemStack result = new ItemStack(recipe.resultMaterial(), recipe.resultAmount());
        Map<Integer, ItemStack> leftover = chest.getInventory().addItem(result);
        if (!leftover.isEmpty()) {
            this.refreshStatusIfChanged(minion, CrafterStatuses.NO_ROOM_FOR_RESULT);
            return false;
        }

        this.removeIngredients(chest, recipe);

        this.commit(minion, type, minion.storage(), Float.NaN);
        this.refreshStatusIfChanged(minion, CrafterStatuses.CRAFTING);
        return true;
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

    // Removes exactly the recipe's ingredient amounts from the chest.
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
            int take = Math.min(needed, item.getAmount());
            int left = item.getAmount() - take;
            chest.getInventory().setItem(slot, left <= 0 ? null : withAmount(item, left));
            int stillNeeded = needed - take;
            if (stillNeeded <= 0) {
                remaining.remove(item.getType());
            }
            else {
                remaining.put(item.getType(), stillNeeded);
            }
        }
    }

    private static ItemStack withAmount(ItemStack item, int amount) {
        ItemStack copy = item.clone();
        copy.setAmount(amount);
        return copy;
    }
}
