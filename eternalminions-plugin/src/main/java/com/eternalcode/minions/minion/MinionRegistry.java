package com.eternalcode.minions.minion;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.LongConsumer;

public final class MinionRegistry implements MinionService {

    private final Long2ObjectOpenHashMap<Minion> minions = new Long2ObjectOpenHashMap<>();
    private final Collection<Minion> minionView = Collections.unmodifiableCollection(this.minions.values());
    private final Map<String, Long2ObjectOpenHashMap<LongOpenHashSet>> minionsByWorldChunk = new HashMap<>();

    public void register(Minion minion) {
        long id = minion.id().value();
        if (this.minions.putIfAbsent(id, minion) != null) {
            throw new IllegalArgumentException("Minion " + id + " is already registered");
        }
        this.index(minion);
    }

    public void replace(Minion minion) {
        long id = minion.id().value();
        if (!this.minions.containsKey(id)) {
            throw new IllegalArgumentException("Minion " + id + " is not registered");
        }
        this.minions.put(id, minion);
    }

    public Optional<Minion> remove(MinionId minionId) {
        Minion removed = this.minions.remove(minionId.value());
        if (removed != null) {
            this.unindex(removed);
        }
        return Optional.ofNullable(removed);
    }

    public Optional<Minion> findMinion(MinionId minionId) {
        return Optional.ofNullable(this.minions.get(minionId.value()));
    }

    public Collection<Minion> minions() {
        return this.minionView;
    }

    public boolean hasMinionAt(
            String worldKey,
            int blockX,
            int blockY,
            int blockZ
    ) {
        Long2ObjectOpenHashMap<LongOpenHashSet> worldChunks =
                this.minionsByWorldChunk.get(worldKey);

        if (worldChunks == null) {
            return false;
        }

        LongOpenHashSet ids = worldChunks.get(
                chunkKey(blockX >> 4, blockZ >> 4)
        );

        if (ids == null) {
            return false;
        }

        LongIterator iterator = ids.iterator();

        while (iterator.hasNext()) {
            Minion minion = this.minions.get(iterator.nextLong());

            if (minion == null) {
                continue;
            }

            MinionPosition position = minion.position();

            if (position.blockX() == blockX
                    && position.blockY() == blockY
                    && position.blockZ() == blockZ) {
                return true;
            }
        }

        return false;
    }

    public void forEachMinionIdInChunk(String worldKey, int chunkX, int chunkZ, LongConsumer action) {
        Long2ObjectOpenHashMap<LongOpenHashSet> worldChunks = this.minionsByWorldChunk.get(worldKey);
        if (worldChunks == null) {
            return;
        }
        LongOpenHashSet ids = worldChunks.get(chunkKey(chunkX, chunkZ));
        if (ids != null) {
            ids.forEach(action);
        }
    }

    @Override
    public Optional<MinionDetails> findById(MinionId minionId) {
        Minion minion = this.minions.get(minionId.value());
        return minion == null ? Optional.empty() : Optional.of(minion.details());
    }

    public int countByOwner(UUID ownerId) {
        int count = 0;
        for (Minion minion : this.minions.values()) {
            if (minion.ownerId().equals(ownerId)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public Collection<MinionDetails> findByOwner(UUID ownerId) {
        List<MinionDetails> details = new ArrayList<>();
        for (Minion minion : this.minions.values()) {
            if (minion.ownerId().equals(ownerId)) {
                details.add(minion.details());
            }
        }
        return List.copyOf(details);
    }

    private void index(Minion minion) {
        String worldKey = minion.position().worldKey();
        long chunkKey = chunkKey(minion.position().blockX() >> 4, minion.position().blockZ() >> 4);
        this.minionsByWorldChunk
            .computeIfAbsent(worldKey, ignored -> new Long2ObjectOpenHashMap<>())
            .computeIfAbsent(chunkKey, ignored -> new LongOpenHashSet())
            .add(minion.id().value());
    }

    private void unindex(Minion minion) {
        String worldKey = minion.position().worldKey();
        Long2ObjectOpenHashMap<LongOpenHashSet> worldChunks = this.minionsByWorldChunk.get(worldKey);
        if (worldChunks == null) {
            return;
        }
        long chunkKey = chunkKey(minion.position().blockX() >> 4, minion.position().blockZ() >> 4);
        LongOpenHashSet ids = worldChunks.get(chunkKey);
        if (ids == null) {
            return;
        }
        ids.remove(minion.id().value());
        if (ids.isEmpty()) {
            worldChunks.remove(chunkKey);
        }
        if (worldChunks.isEmpty()) {
            this.minionsByWorldChunk.remove(worldKey);
        }
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xFFFF_FFFFL);
    }
}
