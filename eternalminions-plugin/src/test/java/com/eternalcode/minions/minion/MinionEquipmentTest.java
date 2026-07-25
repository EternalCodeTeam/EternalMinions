package com.eternalcode.minions.minion;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MinionEquipmentTest {

    @Test
    void createsUpdatedEquipmentWithEmptyTool() {
        MinionEquipment equipment = new MinionEquipment(null).withTool(null);

        assertThat(equipment.tool()).isNull();
    }
}
