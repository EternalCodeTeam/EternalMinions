package com.eternalcode.minions.examples;

import com.eternalcode.minions.event.MinionCreatedEvent;
import com.eternalcode.minions.event.MinionPreCreateEvent;
import com.eternalcode.minions.event.MinionPreRemoveEvent;
import com.eternalcode.minions.event.MinionRemovedEvent;
import com.eternalcode.minions.event.MinionUpdatedEvent;
import com.eternalcode.minions.event.MinionUpgradePurchaseEvent;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class MinionEventExamples implements Listener {

    private final Server server;
    private final Logger logger;

    public MinionEventExamples(Server server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPreCreate(MinionPreCreateEvent event) {
        this.logger.info(
            "Creating " + event.request().behaviorId() + " minion at " + event.request().position()
        );
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreated(MinionCreatedEvent event) {
        this.logger.info(
            "Created minion " + event.minion().details().id() + " via " + event.cause()
        );
    }

    @EventHandler(ignoreCancelled = true)
    public void onPreRemove(MinionPreRemoveEvent event) {
        this.logger.info(
            "Removing minion " + event.minion().details().id() + " via " + event.cause()
        );
    }

    @EventHandler
    public void onRemoved(MinionRemovedEvent event) {
        this.logger.info("Removed minion " + event.minion().details().id());
    }

    @EventHandler
    public void onUpdated(MinionUpdatedEvent event) {
        this.logger.info(
            "Updated minion " + event.current().details().id() + ": " + event.updateType()
        );
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onUpgradePurchase(MinionUpgradePurchaseEvent event) {
        Player player = this.findPlayer(event.actorId());
        if (player == null || !player.hasPermission("eternalminions.examples.block-upgrades")) {
            return;
        }

        event.setCancelled(true);
        ExampleMessages.error(player, "The examples plugin blocked this upgrade purchase.");
    }

    private Player findPlayer(UUID playerId) {
        return playerId == null ? null : this.server.getPlayer(playerId);
    }
}
