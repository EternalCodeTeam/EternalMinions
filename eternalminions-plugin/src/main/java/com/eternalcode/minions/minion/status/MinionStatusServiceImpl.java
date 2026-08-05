package com.eternalcode.minions.minion.status;

import com.eternalcode.minions.minion.MinionId;
import com.eternalcode.minions.minion.MinionRegistry;
import com.eternalcode.minions.event.MinionEventCause;
import com.eternalcode.minions.event.MinionEventDispatcher;
import com.eternalcode.minions.event.MinionUpdatedEvent;
import com.eternalcode.minions.event.MinionUpdateType;
import com.eternalcode.minions.minion.MinionSnapshot;
import com.eternalcode.minions.minion.MinionSnapshotMapper;
import com.eternalcode.minions.status.MinionStatusService;
import java.util.Optional;

public final class MinionStatusServiceImpl implements MinionStatusService {

    private final MinionRegistry minions;
    private final MinionStatusTracker statuses;
    private final MinionSnapshotMapper snapshots;
    private final MinionEventDispatcher events;

    public MinionStatusServiceImpl(
        MinionRegistry minions,
        MinionStatusTracker statuses,
        MinionSnapshotMapper snapshots,
        MinionEventDispatcher events
    ) {
        this.minions = minions;
        this.statuses = statuses;
        this.snapshots = snapshots;
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
        MinionSnapshot previous = this.snapshots.map(minion);
        boolean changed = this.statuses.setStatus(minionId, new MinionStatus(statusKey));
        if (changed) {
            this.fireUpdate(previous, this.snapshots.map(minion));
        }
        return changed;
    }

    @Override
    public boolean clearStatus(MinionId minionId) {
        com.eternalcode.minions.minion.Minion minion = this.minions.findMinion(minionId).orElse(null);
        if (minion == null) {
            return false;
        }
        MinionSnapshot previous = this.snapshots.map(minion);
        boolean changed = !this.statuses.status(minionId).equals(CoreMinionStatuses.IDLE);
        this.statuses.remove(minionId);
        if (changed) {
            this.fireUpdate(previous, this.snapshots.map(minion));
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
