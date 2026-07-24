package com.eternalcode.minions.minion;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public record MinionDrop(Material material, int minAmount, int maxAmount, double chance) {

    public MinionDrop {
        if (material == null) {
            throw new IllegalArgumentException("Minion drop requires a material");
        }
        if (minAmount < 1 || maxAmount < minAmount) {
            throw new IllegalArgumentException("Minion drop amount must be positive and min must not exceed max");
        }
        if (chance <= 0.0D || chance > 1.0D) {
            throw new IllegalArgumentException("Minion drop chance must be within (0, 1]");
        }
    }

    // Appends this drop to the output when its chance rolls; returns whether it produced anything.
    public boolean roll(List<ItemStack> output) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (this.chance < 1.0D && random.nextDouble() >= this.chance) {
            return false;
        }
        int amount = this.minAmount == this.maxAmount
            ? this.minAmount
            : random.nextInt(this.minAmount, this.maxAmount + 1);
        output.add(new ItemStack(this.material, amount));
        return true;
    }
}
