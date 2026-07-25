package com.eternalcode.minions.minion.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class ToolCategoryTest {

    @Test
    void classifiesPickaxesAcrossAllMaterialTiers() {
        assertThat(ToolCategory.of(Material.WOODEN_PICKAXE)).isEqualTo(ToolCategory.PICKAXE);
        assertThat(ToolCategory.of(Material.STONE_PICKAXE)).isEqualTo(ToolCategory.PICKAXE);
        assertThat(ToolCategory.of(Material.COPPER_PICKAXE)).isEqualTo(ToolCategory.PICKAXE);
        assertThat(ToolCategory.of(Material.IRON_PICKAXE)).isEqualTo(ToolCategory.PICKAXE);
        assertThat(ToolCategory.of(Material.GOLDEN_PICKAXE)).isEqualTo(ToolCategory.PICKAXE);
        assertThat(ToolCategory.of(Material.DIAMOND_PICKAXE)).isEqualTo(ToolCategory.PICKAXE);
        assertThat(ToolCategory.of(Material.NETHERITE_PICKAXE)).isEqualTo(ToolCategory.PICKAXE);
    }

    @Test
    void classifiesAxesAcrossAllMaterialTiers() {
        assertThat(ToolCategory.of(Material.WOODEN_AXE)).isEqualTo(ToolCategory.AXE);
        assertThat(ToolCategory.of(Material.STONE_AXE)).isEqualTo(ToolCategory.AXE);
        assertThat(ToolCategory.of(Material.COPPER_AXE)).isEqualTo(ToolCategory.AXE);
        assertThat(ToolCategory.of(Material.IRON_AXE)).isEqualTo(ToolCategory.AXE);
        assertThat(ToolCategory.of(Material.GOLDEN_AXE)).isEqualTo(ToolCategory.AXE);
        assertThat(ToolCategory.of(Material.DIAMOND_AXE)).isEqualTo(ToolCategory.AXE);
        assertThat(ToolCategory.of(Material.NETHERITE_AXE)).isEqualTo(ToolCategory.AXE);
    }

    @Test
    void classifiesShovelsAcrossAllMaterialTiers() {
        assertThat(ToolCategory.of(Material.WOODEN_SHOVEL)).isEqualTo(ToolCategory.SHOVEL);
        assertThat(ToolCategory.of(Material.STONE_SHOVEL)).isEqualTo(ToolCategory.SHOVEL);
        assertThat(ToolCategory.of(Material.COPPER_SHOVEL)).isEqualTo(ToolCategory.SHOVEL);
        assertThat(ToolCategory.of(Material.IRON_SHOVEL)).isEqualTo(ToolCategory.SHOVEL);
        assertThat(ToolCategory.of(Material.GOLDEN_SHOVEL)).isEqualTo(ToolCategory.SHOVEL);
        assertThat(ToolCategory.of(Material.DIAMOND_SHOVEL)).isEqualTo(ToolCategory.SHOVEL);
        assertThat(ToolCategory.of(Material.NETHERITE_SHOVEL)).isEqualTo(ToolCategory.SHOVEL);
    }

    @Test
    void classifiesHoesAcrossAllMaterialTiers() {
        assertThat(ToolCategory.of(Material.WOODEN_HOE)).isEqualTo(ToolCategory.HOE);
        assertThat(ToolCategory.of(Material.STONE_HOE)).isEqualTo(ToolCategory.HOE);
        assertThat(ToolCategory.of(Material.COPPER_HOE)).isEqualTo(ToolCategory.HOE);
        assertThat(ToolCategory.of(Material.IRON_HOE)).isEqualTo(ToolCategory.HOE);
        assertThat(ToolCategory.of(Material.GOLDEN_HOE)).isEqualTo(ToolCategory.HOE);
        assertThat(ToolCategory.of(Material.DIAMOND_HOE)).isEqualTo(ToolCategory.HOE);
        assertThat(ToolCategory.of(Material.NETHERITE_HOE)).isEqualTo(ToolCategory.HOE);
    }

    @Test
    void classifiesFishingRod() {
        assertThat(ToolCategory.of(Material.FISHING_ROD)).isEqualTo(ToolCategory.FISHING_ROD);
    }

    @Test
    void classifiesSwordsAcrossAllMaterialTiersAndTridentAsWeapons() {
        assertThat(ToolCategory.of(Material.WOODEN_SWORD)).isEqualTo(ToolCategory.WEAPON);
        assertThat(ToolCategory.of(Material.STONE_SWORD)).isEqualTo(ToolCategory.WEAPON);
        assertThat(ToolCategory.of(Material.COPPER_SWORD)).isEqualTo(ToolCategory.WEAPON);
        assertThat(ToolCategory.of(Material.IRON_SWORD)).isEqualTo(ToolCategory.WEAPON);
        assertThat(ToolCategory.of(Material.GOLDEN_SWORD)).isEqualTo(ToolCategory.WEAPON);
        assertThat(ToolCategory.of(Material.DIAMOND_SWORD)).isEqualTo(ToolCategory.WEAPON);
        assertThat(ToolCategory.of(Material.NETHERITE_SWORD)).isEqualTo(ToolCategory.WEAPON);
        assertThat(ToolCategory.of(Material.TRIDENT)).isEqualTo(ToolCategory.WEAPON);
    }

    @Test
    void classifiesUnrelatedAndAirMaterialsAsNone() {
        assertThat(ToolCategory.of(Material.DIRT)).isEqualTo(ToolCategory.NONE);
        assertThat(ToolCategory.of(Material.AIR)).isEqualTo(ToolCategory.NONE);
    }

    @Test
    void rejectsNullMaterial() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> ToolCategory.of(null))
            .withMessage("Tool material must not be null");
    }
}
