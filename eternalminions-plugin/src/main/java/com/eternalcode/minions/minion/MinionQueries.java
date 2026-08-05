package com.eternalcode.minions.minion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class MinionQueries implements MinionService {

    private final MinionRegistry minions;
    private final MinionSnapshotMapper snapshots;

    public MinionQueries(MinionRegistry minions, MinionSnapshotMapper snapshots) {
        this.minions = minions;
        this.snapshots = snapshots;
    }

    @Override
    public Optional<MinionDetails> findById(MinionId minionId) {
        return this.minions.findById(minionId);
    }

    @Override
    public Optional<MinionSnapshot> findSnapshotById(MinionId minionId) {
        return this.minions.findMinion(minionId).map(this.snapshots::map);
    }

    @Override
    public Collection<MinionDetails> findAll() {
        List<MinionDetails> details = new ArrayList<>(this.minions.minions().size());
        for (Minion minion : this.minions.minions()) {
            details.add(minion.details());
        }
        return List.copyOf(details);
    }

    @Override
    public Collection<MinionSnapshot> findAllSnapshots() {
        List<MinionSnapshot> result = new ArrayList<>(this.minions.minions().size());
        for (Minion minion : this.minions.minions()) {
            result.add(this.snapshots.map(minion));
        }
        return List.copyOf(result);
    }

    @Override
    public Collection<MinionDetails> findByOwner(UUID ownerId) {
        return this.minions.findByOwner(ownerId);
    }

    @Override
    public int countByOwner(UUID ownerId) {
        return this.minions.countByOwner(ownerId);
    }

    @Override
    public Optional<MinionDetails> findAt(MinionPosition position) {
        return this.minions.findAt(position).map(Minion::details);
    }
}
