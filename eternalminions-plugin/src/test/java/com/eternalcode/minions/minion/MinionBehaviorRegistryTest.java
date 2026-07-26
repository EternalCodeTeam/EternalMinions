package com.eternalcode.minions.minion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eternalcode.minions.minion.impl.miner.MinerConfig;
import java.util.List;
import org.junit.jupiter.api.Test;

class MinionBehaviorRegistryTest {

    @Test
    void replacesAllBehaviorsAsOneSnapshot() {
        MinionBehaviorRegistry registry = new MinionBehaviorRegistry();
        MinionBehavior miner = new TestBehavior("miner");
        MinionBehavior farmer = new TestBehavior("farmer");

        registry.replace(List.of(miner, farmer));

        assertThat(registry.require("MINER")).isSameAs(miner);
        assertThat(registry.find("farmer")).containsSame(farmer);
        assertThat(registry.behaviors()).containsExactly(miner, farmer);
        assertThat(registry.defaultBehavior()).isSameAs(miner);
    }

    @Test
    void preservesPreviousSnapshotWhenReplacementIsInvalid() {
        MinionBehaviorRegistry registry = new MinionBehaviorRegistry();
        MinionBehavior miner = new TestBehavior("miner");
        registry.replace(List.of(miner));

        assertThatThrownBy(() -> registry.replace(List.of(
            new TestBehavior("farmer"),
            new TestBehavior("FARMER")
        )))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Duplicate minion behavior");

        assertThat(registry.behaviors()).containsExactly(miner);
    }

    @Test
    void rejectsUnknownBehavior() {
        MinionBehaviorRegistry registry = new MinionBehaviorRegistry();

        assertThatThrownBy(() -> registry.require("miner"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unknown minion behavior: miner");
    }

    private record TestBehavior(String id) implements MinionBehavior {

        @Override
        public com.eternalcode.minions.config.AbstractMinionConfig config() {
            return new MinerConfig();
        }

        @Override
        public MinionResult execute(MinionContext context) {
            return MinionResult.idle(
                context.minion(),
                com.eternalcode.minions.minion.status.CoreMinionStatuses.IDLE
            );
        }
    }
}
