package com.eternalcode.minions.minion.tool;

import com.eternalcode.minions.minion.Minion;

public record MinionToolPreparation(Minion minion, ToolCheck check) {

    public MinionToolPreparation {
        if (minion == null || check == null) {
            throw new IllegalArgumentException("Prepared minion and tool check are required");
        }
    }
}
