package com.eternalcode.minions.minion.behavior;

import com.eternalcode.minions.event.EventDispatcher;
import com.eternalcode.minions.event.MinionBlockChangeEvent;
import com.eternalcode.minions.event.MinionBlockChangeReason;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.status.MinionStatusTracker;
import org.bukkit.Material;
import org.bukkit.block.Block;

/** Asks listeners whether a minion may change a block. Free when nobody listens. */
public final class MinionBlockChanges {

    private final EventDispatcher events;
    private final MinionStatusTracker statuses;

    public MinionBlockChanges(EventDispatcher events, MinionStatusTracker statuses) {
        this.events = events;
        this.statuses = statuses;
    }

    public boolean observed() {
        return MinionBlockChangeEvent.hasListeners();
    }

    public boolean allows(Minion minion, Block block, Material newMaterial, MinionBlockChangeReason reason) {
        if (!this.observed()) {
            return true;
        }

        MinionBlockChangeEvent event = this.events.fire(new MinionBlockChangeEvent(
            () -> minion.snapshot(this.statuses.status(minion.id())),
            block,
            newMaterial,
            reason
        ));
        return !event.isCancelled();
    }
}
