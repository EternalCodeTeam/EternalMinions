package com.eternalcode.minions.minion;

import com.eternalcode.minions.config.AbstractMinionConfig;

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
