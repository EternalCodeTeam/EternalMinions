package com.eternalcode.minions.minion;

import org.bukkit.World;

public interface MinionBehavior {

    boolean execute(Minion minion, MinionType type, ScheduledMinion scheduledMinion, World world);
}
