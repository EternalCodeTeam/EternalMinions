package com.eternalcode.minions.minion.activity;

import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.activity.rule.MinionActivityRule;
import java.util.List;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class MinionActivityService {

    private final Server server;
    private final MinionActivityBypass bypass;
    private final List<MinionActivityRule> rules;

    public MinionActivityService(Server server, MinionActivityBypass bypass, List<MinionActivityRule> rules) {
        if (server == null || bypass == null || rules == null) {
            throw new IllegalArgumentException("Minion activity service requires a server, bypass and rules");
        }
        this.server = server;
        this.bypass = bypass;
        this.rules = List.copyOf(rules);
    }

    public ActivityDecision evaluate(Minion minion, World world) {
        Player owner = this.server.getPlayer(minion.ownerId());
        if (this.bypass.isBypassed(minion, owner)) {
            return ActivityDecision.ACTIVE;
        }

        long now = System.currentTimeMillis();
        ActivityDecision decision = ActivityDecision.ACTIVE;
        for (MinionActivityRule rule : this.rules) {
            MinionActivityContext context = new MinionActivityContext(minion, world, owner, this.server, now, decision);
            decision = decision.merge(rule.evaluate(context));

            if (decision.frozen()) {
                break;
            }
        }

        return decision;
    }
}
