package com.eternalcode.minions.minion.status;

import com.eternalcode.minions.minion.Minion;
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

    private final MinionRegistry minionRegistry;
    private final MinionStatusTracker statusTracker;
    private final EventDispatcher eventDispatcher;

    public MinionStatusServiceImpl(
        MinionRegistry minionRegistry,
        MinionStatusTracker statusTracker,
        EventDispatcher eventDispatcher
    ) {
        this.minionRegistry = minionRegistry;
        this.statusTracker = statusTracker;
        this.eventDispatcher = eventDispatcher;
    }

    @Override
    public Optional<String> findStatus(MinionId minionId) {
        if (this.minionRegistry.findMinion(minionId).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(this.statusTracker.status(minionId).key());
    }

    @Override
    public boolean setStatus(MinionId minionId, String statusKey) {
        Minion minion = this.minionRegistry.findMinion(minionId).orElse(null);
        if (minion == null) {
            return false;
        }

        MinionSnapshot previous = minion.snapshot(this.statusTracker.status(minionId));
        boolean changed = this.statusTracker.setStatus(minionId, MinionStatus.of(statusKey));
        if (changed) {
            this.fireUpdate(previous, minion.snapshot(this.statusTracker.status(minionId)));
        }
        return changed;
    }

    @Override
    public boolean clearStatus(MinionId minionId) {
        Minion minion = this.minionRegistry.findMinion(minionId).orElse(null);
        if (minion == null) {
            return false;
        }

        MinionSnapshot previous = minion.snapshot(this.statusTracker.status(minionId));
        boolean changed = this.statusTracker.clearStatus(minionId);
        if (changed) {
            this.fireUpdate(previous, minion.snapshot(this.statusTracker.status(minionId)));
        }
        return changed;
    }

    private void fireUpdate(MinionSnapshot previous, MinionSnapshot current) {
        this.eventDispatcher.fire(new MinionUpdatedEvent(
            previous,
            current,
            MinionUpdateType.STATUS,
            MinionEventCause.API,
            null
        ));
    }
}
