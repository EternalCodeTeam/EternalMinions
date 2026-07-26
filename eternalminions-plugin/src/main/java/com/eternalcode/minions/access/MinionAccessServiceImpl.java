package com.eternalcode.minions.access;

import com.eternalcode.minions.minion.MinionDetails;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;

public final class MinionAccessServiceImpl implements MinionAccessService, Listener {

    private final Logger logger;
    private final CopyOnWriteArrayList<Registration> registrations =
            new CopyOnWriteArrayList<>();

    public MinionAccessServiceImpl(Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public boolean canAccess(
            Player player,
            MinionDetails minion,
            MinionAccessAction action
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(minion, "minion");
        Objects.requireNonNull(action, "action");

        if (minion.ownerId().equals(player.getUniqueId())) {
            return true;
        }

        for (Registration registration : this.registrations) {
            if (this.allows(registration, player, minion, action)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public MinionAccessRegistration registerPolicy(
            Plugin plugin,
            MinionAccessPolicy policy
    ) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(policy, "policy");

        if (!plugin.isEnabled()) {
            throw new IllegalStateException(
                    "Cannot register a minion access policy for disabled plugin "
                            + plugin.getName()
            );
        }

        Registration registration = new Registration(plugin, policy);
        this.registrations.add(registration);
        return registration;
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        Plugin disabledPlugin = event.getPlugin();
        this.registrations.removeIf(
                registration -> registration.plugin == disabledPlugin
        );
    }

    private boolean allows(
            Registration registration,
            Player player,
            MinionDetails minion,
            MinionAccessAction action
    ) {
        if (!registration.plugin.isEnabled()) {
            registration.unregister();
            return false;
        }

        try {
            return registration.policy.canAccess(player, minion, action);
        }
        catch (RuntimeException exception) {
            this.logger.log(
                    Level.WARNING,
                    "Minion access policy from "
                            + registration.plugin.getName()
                            + " failed",
                    exception
            );
            return false;
        }
    }

    private final class Registration implements MinionAccessRegistration {

        private final Plugin plugin;
        private final MinionAccessPolicy policy;

        private Registration(
                Plugin plugin,
                MinionAccessPolicy policy
        ) {
            this.plugin = plugin;
            this.policy = policy;
        }

        @Override
        public void unregister() {
            registrations.remove(this);
        }

        @Override
        public boolean isRegistered() {
            return registrations.contains(this);
        }
    }
}
