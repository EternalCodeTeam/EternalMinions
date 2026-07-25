package com.eternalcode.minions.minion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.eternalcode.minions.minion.crafter.CrafterWork;
import com.eternalcode.minions.minion.farmer.FarmerWork;
import com.eternalcode.minions.minion.fisherman.FisherWork;
import com.eternalcode.minions.minion.killer.KillerWork;
import com.eternalcode.minions.minion.lumberjack.LumberjackWork;
import com.eternalcode.minions.minion.tool.ToolCategory;
import com.eternalcode.minions.minion.tool.ToolRequirement;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

class MinionWorkTest {

    private static final LumberjackWork LUMBERJACK_WORK =
        new LumberjackWork(Material.OAK_LOG, Material.OAK_SAPLING, 128);
    private static final FarmerWork FARMER_WORK =
        new FarmerWork(Set.of(Material.WHEAT), Map.of(Material.WHEAT, Material.WHEAT_SEEDS));
    private static final FisherWork FISHER_WORK = new FisherWork(4, 100, 20);
    private static final KillerWork KILLER_WORK = new KillerWork(Set.of(EntityType.ZOMBIE), 4, 20, 1.0);
    private static final CrafterWork CRAFTER_WORK = new CrafterWork(List.of());

    @Test
    void exposesConfiguredToolRequirement() {
        ToolRequirement requirement = new ToolRequirement(
            ToolCategory.PICKAXE,
            Set.of(Material.IRON_PICKAXE),
            5,
            true
        );

        MinionWork work = new MinionWork(
            requirement, 4, Set.of(), Set.of(), Map.of(), 64,
            LUMBERJACK_WORK, FARMER_WORK, FISHER_WORK, KILLER_WORK, CRAFTER_WORK
        );

        assertThat(work.toolRequirement()).isEqualTo(requirement);
    }

    @Test
    void rejectsMissingToolRequirement() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new MinionWork(
                null, 4, Set.of(), Set.of(), Map.of(), 64,
                LUMBERJACK_WORK, FARMER_WORK, FISHER_WORK, KILLER_WORK, CRAFTER_WORK
            ))
            .withMessage("Minion work requires a tool requirement and sell prices");
    }
}
