package com.eternalcode.minions.minion.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class ToolInventoryLocatorTest {

    private final ToolInventoryLocator locator = new ToolInventoryLocator();

    @Test
    void returnsMinusOneWhenStorageHasNoMatchingTool() {
        ToolRequirement requirement = new ToolRequirement(ToolCategory.PICKAXE, Set.of(), 1, true);
        Material[] storage = {
            Material.DIRT,
            null,
            Material.IRON_AXE
        };

        assertThat(this.locator.findInMaterials(storage, requirement)).isEqualTo(-1);
    }

    @Test
    void findsFirstMatchingSlotInStorage() {
        ToolRequirement requirement = new ToolRequirement(ToolCategory.PICKAXE, Set.of(), 1, true);
        Material[] storage = {
            Material.DIRT,
            Material.STONE_PICKAXE,
            Material.DIAMOND_PICKAXE
        };

        assertThat(this.locator.findInMaterials(storage, requirement)).isEqualTo(1);
    }

    @Test
    void skipsNullSlotsWhileScanning() {
        ToolRequirement requirement = new ToolRequirement(ToolCategory.AXE, Set.of(), 1, true);
        Material[] storage = {
            null,
            null,
            Material.IRON_AXE
        };

        assertThat(this.locator.findInMaterials(storage, requirement)).isEqualTo(2);
    }

    @Test
    void respectsWhitelistOverCategoryMatch() {
        ToolRequirement requirement = new ToolRequirement(
            ToolCategory.PICKAXE,
            Set.of(Material.NETHERITE_PICKAXE),
            1,
            true
        );
        Material[] storage = {
            Material.DIAMOND_PICKAXE,
            Material.NETHERITE_PICKAXE
        };

        assertThat(this.locator.findInMaterials(storage, requirement)).isEqualTo(1);
    }
}
