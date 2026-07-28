package com.eternalcode.minions.minion.activity;

import com.eternalcode.minions.minion.Minion;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;

public record MinionActivityContext(
        Minion minion,
        World world,
        Player owner,
        Server server,
        long currentTimeMillis,
        ActivityDecision decisionSoFar
) {

    public MinionActivityContext {
        if (minion == null || world == null || server == null || decisionSoFar == null) {
            throw new IllegalArgumentException(
                    "Minion activity context requires minion, world, server and prior decision");
        }
    }
}
