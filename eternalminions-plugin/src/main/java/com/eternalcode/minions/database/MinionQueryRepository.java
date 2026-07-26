package com.eternalcode.minions.database;

import com.eternalcode.commons.scheduler.Scheduler;
import com.eternalcode.minions.minion.MinionDirection;
import com.eternalcode.minions.minion.MinionId;
import com.eternalcode.minions.minion.storage.MinionSettings;
import com.eternalcode.minions.minion.upgrade.UpgradeKind;
import com.eternalcode.minions.minion.MiningMode;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

final class MinionQueryRepository extends AbstractRepositoryOrmLite {

    private static final int MAXIMUM_IN_PARAMETERS = 500;

    MinionQueryRepository(DatabaseManager databaseManager, Scheduler scheduler) {
        super(databaseManager, scheduler);
    }

    private static Map<Long, MinionStateTable> indexStates(List<MinionStateTable> rows) {
        Map<Long, MinionStateTable> states = new HashMap<>();
        for (MinionStateTable row : rows) {
            states.put(row.minionId(), row);
        }
        return states;
    }

    private static Map<Long, MinionSettings> indexSettings(List<MinionSettingsTable> rows) {
        Map<Long, MinionSettings> settings = new HashMap<>();
        for (MinionSettingsTable row : rows) {
            settings.put(row.minionId(), parseSettings(row));
        }
        return settings;
    }

    private static Map<Long, byte[]> indexEquipment(List<MinionEquipmentTable> rows) {
        Map<Long, byte[]> tools = new HashMap<>();
        for (MinionEquipmentTable row : rows) {
            if (!row.slot().equals(MinionEquipmentSlot.TOOL.name())) {
                continue;
            }
            if (row.serializedItem().length == 0) {
                continue;
            }
            tools.put(row.minionId(), row.serializedItem());
        }
        return tools;
    }

    private static Map<Long, List<StoredItemData>> indexStorage(List<MinionStorageTable> rows) {
        Map<Long, List<StoredItemData>> storage = new HashMap<>();
        for (MinionStorageTable row : rows) {
            if (row.serializedItem().length == 0) {
                continue;
            }
            storage.computeIfAbsent(row.minionId(), ignored -> new ArrayList<>())
                    .add(new StoredItemData(row.slot(), row.serializedItem()));
        }
        for (List<StoredItemData> items : storage.values()) {
            items.sort(Comparator.comparingInt(StoredItemData::slot));
        }
        return storage;
    }

    private static Map<Long, Map<String, Integer>> indexUpgrades(List<MinionUpgradeTable> rows) {
        Map<Long, Map<String, Integer>> upgrades = new HashMap<>();
        for (MinionUpgradeTable row : rows) {
            if (row.tier() <= 0) {
                continue;
            }
            try {
                new UpgradeKind(row.upgradeType());
            }
            catch (IllegalArgumentException ignored) {
                continue;
            }
            upgrades.computeIfAbsent(row.minionId(), ignored -> new LinkedHashMap<>())
                    .put(row.upgradeType(), row.tier());
        }
        return upgrades;
    }

    private static Map<Long, MinionData.ChestPositionData> indexChests(List<MinionChestLinkTable> rows) {
        Map<Long, MinionData.ChestPositionData> chests = new HashMap<>();
        for (MinionChestLinkTable row : rows) {
            chests.put(
                    row.minionId(),
                    new MinionData.ChestPositionData(row.worldKey(), row.blockX(), row.blockY(), row.blockZ())
            );
        }
        return chests;
    }

    private static MinionSettings parseSettings(MinionSettingsTable row) {
        MinionSettings defaults = MinionSettings.defaults();
        MinionDirection direction = parseDirection(row.direction(), defaults.direction());
        MiningMode miningMode = parseMiningMode(row.miningMode(), defaults.miningMode());
        return new MinionSettings(direction, miningMode);
    }

    private static MinionDirection parseDirection(String value, MinionDirection fallback) {
        try {
            return MinionDirection.valueOf(value);
        }
        catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    private static MiningMode parseMiningMode(String value, MiningMode fallback) {
        try {
            return MiningMode.valueOf(value);
        }
        catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    CompletableFuture<Optional<MinionData>> findById(MinionId minionId) {
        if (minionId == null) {
            throw new IllegalArgumentException("Minion id is required");
        }

        return this.<MinionTable, Long, Optional<MinionData>>action(
                MinionTable.class, minions -> {
                    MinionTable minion = minions.queryForId(minionId.value());
                    if (minion == null) {
                        return Optional.empty();
                    }
                    return Optional.of(this.load(List.of(minion)).getFirst());
                });
    }

    CompletableFuture<List<MinionData>> findByOwner(UUID ownerId) {
        if (ownerId == null) {
            throw new IllegalArgumentException("Owner id is required");
        }

        return this.<MinionTable, Long, List<MinionData>>action(
                MinionTable.class, minions -> {
                    QueryBuilder<MinionTable, Long> query = minions.queryBuilder();
                    query.orderBy("id", true).where().eq("owner_id", ownerId.toString());
                    return this.load(query.query());
                });
    }

    CompletableFuture<List<MinionData>> findByChunk(String worldKey, int chunkX, int chunkZ) {
        if (worldKey == null || worldKey.isBlank()) {
            throw new IllegalArgumentException("World key is required");
        }

        int minimumBlockX = chunkX << 4;
        int minimumBlockZ = chunkZ << 4;
        int maximumBlockX = minimumBlockX + 15;
        int maximumBlockZ = minimumBlockZ + 15;
        return this.<MinionTable, Long, List<MinionData>>action(
                MinionTable.class, minions -> {
                    QueryBuilder<MinionTable, Long> query = minions.queryBuilder();
                    query.orderBy("id", true);
                    query.where()
                            .eq("world_key", worldKey)
                            .and()
                            .between("block_x", minimumBlockX, maximumBlockX)
                            .and()
                            .between("block_z", minimumBlockZ, maximumBlockZ);
                    return this.load(query.query());
                });
    }

    CompletableFuture<List<MinionData>> loadAll() {
        return this.<MinionTable, Long, List<MinionData>>action(
                MinionTable.class, minions -> {
                    QueryBuilder<MinionTable, Long> query = minions.queryBuilder();
                    query.orderBy("id", true);
                    return this.load(query.query());
                });
    }

    private List<MinionData> load(List<MinionTable> minions) throws SQLException {
        if (minions.isEmpty()) {
            return List.of();
        }

        List<Long> minionIds = new ArrayList<>(minions.size());
        for (MinionTable minion : minions) {
            minionIds.add(minion.id());
        }

        Map<Long, MinionStateTable> states = indexStates(this.queryByMinionIds(MinionStateTable.class, minionIds));
        Map<Long, MinionSettings> settings =
                indexSettings(this.queryByMinionIds(MinionSettingsTable.class, minionIds));
        Map<Long, byte[]> tools =
                indexEquipment(this.queryByMinionIds(MinionEquipmentTable.class, minionIds));
        Map<Long, List<StoredItemData>> storage =
                indexStorage(this.queryByMinionIds(MinionStorageTable.class, minionIds));
        Map<Long, Map<String, Integer>> upgrades =
                indexUpgrades(this.queryByMinionIds(MinionUpgradeTable.class, minionIds));
        Map<Long, MinionData.ChestPositionData> chests =
                indexChests(this.queryByMinionIds(MinionChestLinkTable.class, minionIds));

        List<MinionData> loaded = new ArrayList<>(minions.size());
        for (MinionTable minion : minions) {
            loaded.add(this.assemble(
                    minion,
                    states,
                    settings,
                    tools,
                    storage,
                    upgrades,
                    chests
            ));
        }
        return List.copyOf(loaded);
    }

    private MinionData assemble(
            MinionTable minion,
            Map<Long, MinionStateTable> states,
            Map<Long, MinionSettings> settings,
            Map<Long, byte[]> tools,
            Map<Long, List<StoredItemData>> storage,
            Map<Long, Map<String, Integer>> upgrades,
            Map<Long, MinionData.ChestPositionData> chests
    ) {
        MinionStateTable state = states.get(minion.id());
        if (state == null) {
            throw new MinionRepositoryException("Missing state row for minion " + minion.id());
        }
        return new MinionData(
                minion.id(),
                UUID.fromString(minion.ownerId()),
                minion.behaviorId(),
                minion.worldKey(),
                minion.blockX(),
                minion.blockY(),
                minion.blockZ(),
                state.active(),
                state.level(),
                state.progress(),
                tools.getOrDefault(minion.id(), new byte[0]),
                storage.getOrDefault(minion.id(), List.of()),
                upgrades.getOrDefault(minion.id(), Map.of()),
                chests.get(minion.id()),
                settings.getOrDefault(minion.id(), MinionSettings.defaults()),
                minion.createdAt()
        );
    }

    private <T> List<T> queryByMinionIds(Class<T> type, List<Long> minionIds) throws SQLException {
        Dao<T, Object> rows = this.databaseManager.getDao(type);
        List<T> loaded = new ArrayList<>();
        for (int offset = 0; offset < minionIds.size(); offset += MAXIMUM_IN_PARAMETERS) {
            int end = Math.min(offset + MAXIMUM_IN_PARAMETERS, minionIds.size());
            QueryBuilder<T, Object> query = rows.queryBuilder();
            query.where().in("minion_id", minionIds.subList(offset, end));
            loaded.addAll(query.query());
        }
        return loaded;
    }
}
