package com.eternalcode.minions.minion.tool;

import static org.assertj.core.api.Assertions.assertThat;

import com.eternalcode.minions.minion.status.CoreMinionStatuses;
import java.util.Set;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class ToolValidationServiceTest {

    private final ToolValidationService validationService = new ToolValidationService();

    @Test
    void stopsRequiredToolWhenItemIsMissing() {
        ToolRequirement requirement = new ToolRequirement(ToolCategory.PICKAXE, Set.of(), 1, true);

        ToolCheck check = this.validationService.validate(requirement, null);

        assertThat(check).isEqualTo(new ToolCheck.Stopped(CoreMinionStatuses.NO_TOOL));
    }

    @Test
    void stopsRequiredToolWhenCategoryDoesNotMatch() {
        ToolRequirement requirement = new ToolRequirement(ToolCategory.PICKAXE, Set.of(), 1, true);

        ToolCheck check = this.validationService.validateMaterial(requirement, Material.IRON_AXE);

        assertThat(check).isEqualTo(new ToolCheck.Stopped(CoreMinionStatuses.NO_TOOL));
    }

    @Test
    void acceptsMatchingRequiredTool() {
        ToolRequirement requirement = new ToolRequirement(ToolCategory.PICKAXE, Set.of(), 1, true);

        ToolCheck check = this.validationService.validateMaterial(requirement, Material.IRON_PICKAXE);

        assertThat(check).isInstanceOf(ToolCheck.Ready.class);
    }

    @Test
    void acceptsMissingOptionalTool() {
        ToolRequirement requirement = new ToolRequirement(ToolCategory.ANY, Set.of(), 1, false);

        ToolCheck check = this.validationService.validate(requirement, null);

        assertThat(check).isEqualTo(ToolCheck.Ready.EMPTY);
    }
}
