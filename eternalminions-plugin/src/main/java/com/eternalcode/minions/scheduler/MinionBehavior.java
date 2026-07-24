package com.eternalcode.minions.scheduler;

import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionType;
import org.bukkit.World;

public interface MinionBehavior {

    boolean execute(Minion minion, MinionType type, ScheduledMinion scheduledMinion, World world);
}
