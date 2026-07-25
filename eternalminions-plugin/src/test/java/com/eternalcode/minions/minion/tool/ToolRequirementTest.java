package com.eternalcode.minions.minion.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumSet;
import java.util.Set;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class ToolRequirementTest {

    @Test
    void matchesAnyMaterialOfTheRequiredCategoryWhenWhitelistIsEmpty() {
        ToolRequirement requirement = new ToolRequirement(ToolCategory.PICKAXE, Set.of(), 1, true);

        assertThat(requirement.matches(Material.STONE_PICKAXE)).isTrue();
        assertThat(requirement.matches(Material.DIAMOND_PICKAXE)).isTrue();
        assertThat(requirement.matches(Material.IRON_AXE)).isFalse();
    }

    @Test
    void restrictsMatchingToWhitelistWhenPresent() {
        ToolRequirement requirement = new ToolRequirement(
            ToolCategory.PICKAXE,
            Set.of(Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE),
            1,
            true
        );

        assertThat(requirement.matches(Material.DIAMOND_PICKAXE)).isTrue();
        assertThat(requirement.matches(Material.STONE_PICKAXE)).isFalse();
    }

    @Test
    void anyCategoryAcceptsAnyNonAirMaterial() {
        ToolRequirement requirement = new ToolRequirement(ToolCategory.ANY, Set.of(), 1, false);

        assertThat(requirement.matches(Material.STICK)).isTrue();
        assertThat(requirement.matches(Material.DIAMOND_PICKAXE)).isTrue();
        assertThat(requirement.matches(Material.AIR)).isFalse();
    }

    @Test
    void anyCategoryRespectsWhitelist() {
        ToolRequirement requirement = new ToolRequirement(
            ToolCategory.ANY,
            Set.of(Material.STICK),
            1,
            false
        );

        assertThat(requirement.matches(Material.STICK)).isTrue();
        assertThat(requirement.matches(Material.DIAMOND_PICKAXE)).isFalse();
    }

    @Test
    void noneCategoryNeverMatchesAnyMaterial() {
        ToolRequirement requirement = new ToolRequirement(
            ToolCategory.NONE,
            Set.of(Material.STICK),
            1,
            false
        );

        assertThat(requirement.matches(Material.STICK)).isFalse();
        assertThat(requirement.matches(Material.AIR)).isFalse();
    }

    @Test
    void rejectsNonPositiveMinimumDurability() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new ToolRequirement(ToolCategory.PICKAXE, Set.of(), 0, true))
            .withMessage("Tool requirement minimum durability to keep must be positive");
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new ToolRequirement(ToolCategory.PICKAXE, Set.of(), -1, true))
            .withMessage("Tool requirement minimum durability to keep must be positive");
    }

    @Test
    void rejectsNullCategory() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new ToolRequirement(null, Set.of(), 1, true))
            .withMessage("Tool requirement category must not be null");
    }

    @Test
    void rejectsNullWhitelist() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new ToolRequirement(ToolCategory.PICKAXE, null, 1, true))
            .withMessage("Tool requirement allowed materials must not be null");
    }

    @Test
    void defensivelyCopiesWhitelist() {
        Set<Material> allowedMaterials = EnumSet.of(Material.DIAMOND_PICKAXE);
        ToolRequirement requirement = new ToolRequirement(
            ToolCategory.PICKAXE,
            allowedMaterials,
            1,
            true
        );

        allowedMaterials.add(Material.STONE_PICKAXE);

        assertThat(requirement.allowedMaterials()).containsExactly(Material.DIAMOND_PICKAXE);
        assertThat(requirement.matches(Material.STONE_PICKAXE)).isFalse();
    }

    @Test
    void exposesImmutableWhitelist() {
        ToolRequirement requirement = new ToolRequirement(
            ToolCategory.PICKAXE,
            Set.of(Material.DIAMOND_PICKAXE),
            1,
            true
        );

        assertThatThrownBy(() -> requirement.allowedMaterials().add(Material.STONE_PICKAXE))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsNullMaterialWhenMatching() {
        ToolRequirement requirement = new ToolRequirement(ToolCategory.ANY, Set.of(), 1, false);

        assertThatIllegalArgumentException()
            .isThrownBy(() -> requirement.matches(null))
            .withMessage("Tool material must not be null");
    }
}
