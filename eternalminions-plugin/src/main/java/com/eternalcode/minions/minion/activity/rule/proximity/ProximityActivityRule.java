package com.eternalcode.minions.minion.activity.rule.proximity;

import com.eternalcode.minions.minion.MinionPosition;
import com.eternalcode.minions.minion.activity.ActivityDecision;
import com.eternalcode.minions.minion.activity.MinionActivityContext;
import com.eternalcode.minions.minion.activity.config.PresenceMode;
import com.eternalcode.minions.minion.activity.rule.MinionActivityRule;
import com.eternalcode.minions.minion.status.CoreMinionStatuses;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class ProximityActivityRule implements MinionActivityRule {

    private final ProximityConfig config;

    public ProximityActivityRule(ProximityConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Proximity rule config is required");
        }
        this.config = config;
    }

    @Override
    public ActivityDecision evaluate(MinionActivityContext context) {
        if (!this.config.enabled) {
            return ActivityDecision.ACTIVE;
        }
        if (this.config.skipIfOfflineRuleTriggered && !context.decisionSoFar().active()) {
            return ActivityDecision.ACTIVE;
        }

        int radiusBlocks = this.config.radiusBlocksFor(context.minion().behaviorId());
        if (this.isPresent(context, radiusBlocks)) {
            return ActivityDecision.ACTIVE;
        }

        return ActivityDecision.of(this.config.mode, this.config.slowMultiplier, CoreMinionStatuses.AWAY);
    }

    private boolean isPresent(MinionActivityContext context, int radiusBlocks) {
        double radiusSquared = (double) radiusBlocks * radiusBlocks;

        if (this.config.presence == PresenceMode.OWNER_ONLY) {
            Player owner = context.owner();
            return owner != null && this.withinRadius(context, owner, radiusSquared);
        }

        for (Player player : context.server().getOnlinePlayers()) {
            if (this.withinRadius(context, player, radiusSquared)) {
                return true;
            }
        }
        return false;
    }

    private boolean withinRadius(MinionActivityContext context, Player player, double radiusSquared) {
        if (!player.getWorld().equals(context.world())) {
            return false;
        }

        MinionPosition position = context.minion().position();
        Location playerLocation = player.getLocation();
        double deltaX = playerLocation.getX() - (position.blockX() + 0.5D);
        double deltaY = playerLocation.getY() - position.blockY();
        double deltaZ = playerLocation.getZ() - (position.blockZ() + 0.5D);
        return (deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ) <= radiusSquared;
    }
}
