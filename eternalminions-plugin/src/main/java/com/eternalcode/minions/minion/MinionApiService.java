package com.eternalcode.minions.minion;

import com.eternalcode.minions.minion.storage.MinionSettings;
import com.eternalcode.minions.event.MinionEventCause;
import com.eternalcode.minions.minion.storage.MinionStorage;
import com.eternalcode.minions.minion.upgrade.CoreUpgradeKinds;
import com.eternalcode.minions.minion.upgrade.MinionUpgrades;
import com.eternalcode.minions.minion.upgrade.UpgradeKind;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;
import org.bukkit.inventory.ItemStack;

public final class MinionApiService implements MinionService, MinionManagementService {

    private final MinionRegistry minions;
    private final MinionLifecycleService lifecycle;
    private final MinionBehaviorRegistry behaviors;
    private final MinionIdSequence ids;
    private final MinionSnapshotMapper snapshots;

    public MinionApiService(
        MinionRegistry minions,
        MinionLifecycleService lifecycle,
        MinionBehaviorRegistry behaviors,
        MinionIdSequence ids,
        MinionSnapshotMapper snapshots
    ) {
        this.minions = minions;
        this.lifecycle = lifecycle;
        this.behaviors = behaviors;
        this.ids = ids;
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
        List<MinionSnapshot> snapshots = new ArrayList<>(this.minions.minions().size());
        for (Minion minion : this.minions.minions()) {
            snapshots.add(this.snapshots.map(minion));
        }
        return List.copyOf(snapshots);
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
        for (Minion minion : this.minions.minions()) {
            if (minion.position().equals(position)) {
                return Optional.of(minion.details());
            }
        }
        return Optional.empty();
    }

    @Override
    public MinionSnapshot create(MinionCreateRequest request) {
        if (this.findAt(request.position()).isPresent()) {
            throw new IllegalArgumentException("A minion already occupies " + request.position());
        }

        MinionBehavior behavior = this.behaviors.require(request.behaviorId());
        Minion minion = new Minion(
            this.ids.next(),
            request.ownerId(),
            behavior.id(),
            request.position(),
            MinionProgress.start(),
            MinionEquipment.empty(),
            new MinionStorage(behavior.config().storageCapacity(MinionUpgrades.none())),
            MinionUpgrades.none(),
            null,
            new MinionSettings(request.direction())
        );
        if (!this.lifecycle.add(minion, MinionEventCause.API, null)) {
            throw new MinionOperationCancelledException("Minion creation was cancelled by an event listener");
        }
        return this.snapshots.map(minion);
    }

    @Override
    public boolean remove(MinionId minionId) {
        return this.lifecycle.remove(minionId, MinionEventCause.API, null);
    }

    @Override
    public Optional<MinionSnapshot> setDirection(MinionId minionId, MinionDirection direction) {
        if (direction == null) {
            throw new IllegalArgumentException("Direction is required");
        }
        return this.updateSettings(minionId, minion -> minion.withSettings(new MinionSettings(direction)));
    }

    @Override
    public Optional<MinionSnapshot> setChestPosition(MinionId minionId, MinionPosition position) {
        Minion minion = this.minions.findMinion(minionId).orElse(null);
        if (minion == null) {
            return Optional.empty();
        }
        Minion updated = minion.withChestPosition(position);
        this.lifecycle.updateChestLink(updated, MinionEventCause.API, null);
        return Optional.of(this.snapshots.map(updated));
    }

    @Override
    public Optional<MinionSnapshot> setStorageItem(MinionId minionId, int slot, ItemStack item) {
        Minion minion = this.minions.findMinion(minionId).orElse(null);
        if (minion == null) {
            return Optional.empty();
        }
        Minion updated = minion.withStorage(minion.storage().withItem(slot, item));
        this.lifecycle.updateStorage(updated, MinionEventCause.API, null);
        return Optional.of(this.snapshots.map(updated));
    }

    @Override
    public Optional<MinionSnapshot> setTool(MinionId minionId, ItemStack tool) {
        Minion minion = this.minions.findMinion(minionId).orElse(null);
        if (minion == null) {
            return Optional.empty();
        }
        Minion updated = minion.withEquipment(minion.equipment().withTool(tool));
        this.lifecycle.updateEquipment(updated, MinionEventCause.API, null);
        return Optional.of(this.snapshots.map(updated));
    }

    @Override
    public Optional<MinionSnapshot> setUpgradeTier(MinionId minionId, String upgradeId, int tier) {
        if (upgradeId == null || upgradeId.isBlank()) {
            throw new IllegalArgumentException("Upgrade id must not be blank");
        }
        if (tier < 0) {
            throw new IllegalArgumentException("Upgrade tier cannot be negative");
        }

        Minion minion = this.minions.findMinion(minionId).orElse(null);
        if (minion == null) {
            return Optional.empty();
        }
        UpgradeKind kind = new UpgradeKind(upgradeId.toUpperCase(Locale.ROOT));
        MinionBehavior behavior = this.behaviors.require(minion.behaviorId());
        if (tier > behavior.config().maxUpgradeTier(kind)) {
            throw new IllegalArgumentException("Upgrade tier exceeds behavior maximum");
        }

        Minion updated = minion.withUpgrades(minion.upgrades().withTier(kind, tier));
        if (kind.equals(CoreUpgradeKinds.CAPACITY)) {
            int capacity = behavior.storageCapacity(updated);
            if (capacity > updated.storage().capacity()) {
                updated = updated.withStorage(updated.storage().resized(capacity));
            }
        }
        this.lifecycle.updateUpgrade(updated, kind, MinionEventCause.API, null);
        return Optional.of(this.snapshots.map(updated));
    }

    @Override
    public Optional<MinionSnapshot> setProgress(MinionId minionId, int level, long progress) {
        Minion minion = this.minions.findMinion(minionId).orElse(null);
        if (minion == null) {
            return Optional.empty();
        }
        Minion updated = minion.withProgress(new MinionProgress(level, progress));
        this.lifecycle.updateState(updated, MinionEventCause.API, null);
        return Optional.of(this.snapshots.map(updated));
    }

    private Optional<MinionSnapshot> updateSettings(
        MinionId minionId,
        UnaryOperator<Minion> update
    ) {
        Minion minion = this.minions.findMinion(minionId).orElse(null);
        if (minion == null) {
            return Optional.empty();
        }
        Minion updated = update.apply(minion);
        this.lifecycle.updateSettings(updated, MinionEventCause.API, null);
        return Optional.of(this.snapshots.map(updated));
    }
}
