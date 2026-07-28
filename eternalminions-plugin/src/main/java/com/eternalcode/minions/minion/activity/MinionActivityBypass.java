package com.eternalcode.minions.minion.activity;

import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.activity.config.ActivityBypassConfig;
import org.bukkit.entity.Player;

public final class MinionActivityBypass {

    private final ActivityBypassConfig config;

    public MinionActivityBypass(ActivityBypassConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Activity bypass config is required");
        }
        this.config = config;
    }

    public boolean isBypassed(Minion minion, Player owner) {
        if (this.config.exemptMinionTypes.contains(minion.behaviorId())) {
            return true;
        }
        return owner != null && !this.config.permission.isBlank() && owner.hasPermission(this.config.permission);
    }
}
