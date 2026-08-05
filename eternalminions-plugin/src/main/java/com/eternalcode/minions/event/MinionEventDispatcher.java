package com.eternalcode.minions.event;

import org.bukkit.Server;
import org.bukkit.event.Event;

public final class MinionEventDispatcher {

    private final Server server;

    public MinionEventDispatcher(Server server) {
        this.server = server;
    }

    public <E extends Event> E fire(E event) {
        this.server.getPluginManager().callEvent(event);
        return event;
    }
}
