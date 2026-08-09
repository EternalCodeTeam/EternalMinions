package com.eternalcode.minions.minion.behavior;

import com.eternalcode.minions.config.AbstractMinionConfig;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionContext;
import com.eternalcode.minions.minion.MinionResult;

public interface MinionBehavior {

    String id();

    AbstractMinionConfig config();

    MinionResult execute(MinionContext context);

    default long workInterval(Minion minion) {
        return this.config().workInterval(minion.upgrades());
    }

    default long idleInterval() {
        return this.config().idleIntervalTicks;
    }

    default int storageCapacity(Minion minion) {
        return this.config().storageCapacity(minion.upgrades());
    }
}
