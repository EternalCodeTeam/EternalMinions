package com.eternalcode.minions.minion;

import com.eternalcode.minions.minion.status.MinionStatusTracker;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class MinionServiceImpl implements MinionService {

    private final MinionRegistry minions;
    private final MinionStatusTracker statuses;

    public MinionServiceImpl(MinionRegistry minions, MinionStatusTracker statuses) {
        this.minions = minions;
        this.statuses = statuses;
    }

    @Override
    public Optional<MinionDetails> findById(MinionId minionId) {
        return this.minions.findMinion(minionId).map(Minion::details);
    }

    @Override
    public Optional<MinionSnapshot> findSnapshotById(MinionId minionId) {
        return this.minions.findMinion(minionId).map(this::snapshot);
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
            result.add(this.snapshot(minion));
        }
        return List.copyOf(result);
    }

    @Override
    public Collection<MinionDetails> findByOwner(UUID ownerId) {
        List<MinionDetails> details = new ArrayList<>();
        for (Minion minion : this.minions.minions()) {
            if (minion.ownerId().equals(ownerId)) {
                details.add(minion.details());
            }
        }
        return List.copyOf(details);
    }

    @Override
    public int countByOwner(UUID ownerId) {
        return this.minions.countByOwner(ownerId);
    }

    @Override
    public Optional<MinionDetails> findAt(MinionPosition position) {
        return this.minions.findAt(position).map(Minion::details);
    }

    private MinionSnapshot snapshot(Minion minion) {
        return minion.snapshot(this.statuses.status(minion.id()));
    }
}
