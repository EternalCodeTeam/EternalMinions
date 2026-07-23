package com.eternalcode.minions.database;

import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionId;
import com.eternalcode.minions.minion.MinionRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import org.bukkit.plugin.Plugin;

public final class MinionPersistenceService {

    private final Plugin plugin;
    private final MinionRegistry registry;
    private final MinionRepository repository;
    private final DirtyMinionTracker dirtyMinions = new DirtyMinionTracker();
    private final AtomicBoolean flushRunning = new AtomicBoolean();

    public MinionPersistenceService(Plugin plugin, MinionRegistry registry, MinionRepository repository) {
        this.plugin = plugin;
        this.registry = registry;
        this.repository = repository;
    }

    public void flushDirty() {
        if (!this.repository.ready()) {
            return;
        }
        if (!this.flushRunning.compareAndSet(false, true)) {
            return;
        }

        List<PendingSave> pending = new ArrayList<>();
        List<MinionData> data = new ArrayList<>();
        for (Minion minion : this.registry.minions()) {
            if (!this.dirtyMinions.isDirty(minion.id())) {
                continue;
            }
            long version = this.dirtyMinions.version(minion.id());
            pending.add(new PendingSave(minion.id(), version));
            data.add(MinionData.capture(minion));
        }

        if (data.isEmpty()) {
            this.flushRunning.set(false);
            return;
        }

        this.repository.save(data).whenComplete((ignored, error) -> {
            if (!this.plugin.isEnabled()) {
                this.flushRunning.set(false);
                return;
            }
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
            this.flushRunning.set(false);
            if (error != null) {
                this.plugin.getLogger().log(Level.SEVERE, "Unable to flush dirty minions", error);
                return;
            }
            for (PendingSave save : pending) {
                this.dirtyMinions.markSaved(save.minionId(), save.version());
            }
            });
        });
    }

    public void changed(Minion minion) {
        this.dirtyMinions.changed(minion.id());
    }

    public void forget(MinionId minionId) {
        this.dirtyMinions.remove(minionId);
    }

    public void saveNow(Minion minion) {
        if (!this.repository.ready()) {
            return;
        }
        long version = this.dirtyMinions.changed(minion.id());
        this.repository.save(List.of(MinionData.capture(minion))).whenComplete((ignored, error) -> {
            if (error != null) {
                this.plugin.getLogger().log(Level.SEVERE, "Unable to save minion " + minion.id().value(), error);
                return;
            }
            if (this.plugin.isEnabled()) {
                this.plugin.getServer().getScheduler().runTask(this.plugin,
                    () -> this.dirtyMinions.markSaved(minion.id(), version));
            }
        });
    }

    private record PendingSave(MinionId minionId, long version) {
    }
}
