package com.eternalcode.minions.minion.status;

import com.eternalcode.minions.minion.MinionId;
import com.eternalcode.minions.minion.MinionRegistry;
import com.eternalcode.minions.event.MinionEventCause;
import com.eternalcode.minions.event.EventDispatcher;
import com.eternalcode.minions.event.MinionUpdatedEvent;
import com.eternalcode.minions.event.MinionUpdateType;
import com.eternalcode.minions.minion.MinionSnapshot;
import com.eternalcode.minions.status.MinionStatusService;
import java.util.Optional;

public final class MinionStatusServiceImpl implements MinionStatusService {

    private final MinionRegistry minions;
    private final MinionStatusTracker statuses;
    private final EventDispatcher events;

    public MinionStatusServiceImpl(
        MinionRegistry minions,
        MinionStatusTracker statuses,
        EventDispatcher events
    ) {
        this.minions = minions;
        this.statuses = statuses;
        this.events = events;
    }

    @Override
    public Optional<String> findStatus(MinionId minionId) {
        if (this.minions.findMinion(minionId).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(this.statuses.status(minionId).key());
    }

    @Override
    public boolean setStatus(MinionId minionId, String statusKey) {
        com.eternalcode.minions.minion.Minion minion = this.minions.findMinion(minionId).orElse(null);
        if (minion == null) {
            return false;
        }
        MinionSnapshot previous = minion.snapshot(this.statuses.status(minionId));
        boolean changed = this.statuses.setStatus(minionId, new MinionStatus(statusKey));
        if (changed) {
            this.fireUpdate(previous, minion.snapshot(this.statuses.status(minionId)));
        }
        return changed;
    }

    @Override
    public boolean clearStatus(MinionId minionId) {
        com.eternalcode.minions.minion.Minion minion = this.minions.findMinion(minionId).orElse(null);
        if (minion == null) {
            return false;
        }
        MinionSnapshot previous = minion.snapshot(this.statuses.status(minionId));
        boolean changed = !this.statuses.status(minionId).equals(CoreMinionStatuses.IDLE);
        this.statuses.remove(minionId);
        if (changed) {
            this.fireUpdate(previous, minion.snapshot(this.statuses.status(minionId)));
        }
        return changed;
    }

    private void fireUpdate(MinionSnapshot previous, MinionSnapshot current) {
        this.events.fire(new MinionUpdatedEvent(
            previous,
            current,
            MinionUpdateType.STATUS,
            MinionEventCause.API,
            null
        ));
    }
}
