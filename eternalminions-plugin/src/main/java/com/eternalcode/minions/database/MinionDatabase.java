package com.eternalcode.minions.database;

import com.eternalcode.minions.database.repository.MinionEquipmentRepository;
import com.eternalcode.minions.database.repository.MinionActionRepository;
import com.eternalcode.minions.database.repository.MinionRepository;
import com.eternalcode.minions.database.repository.MinionStateRepository;
import com.eternalcode.minions.minion.storage.MinionChestLinkRepository;
import com.eternalcode.minions.minion.storage.MinionSettingsRepository;
import com.eternalcode.minions.minion.storage.MinionStorageRepository;
import com.eternalcode.minions.minion.upgrade.MinionUpgradeRepository;
import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MinionDatabase {

    private final Logger logger;
    private final DatabaseScheduler scheduler;
    private final DatabaseManager manager;
    private final MinionRepository minions;
    private final MinionStateRepository states;
    private final MinionSettingsRepository settings;
    private final MinionEquipmentRepository equipment;
    private final MinionStorageRepository storage;
    private final MinionUpgradeRepository upgrades;
    private final MinionChestLinkRepository chestLinks;
    private final MinionPersistenceService persistence;

    private MinionDatabase(
            Logger logger,
            DatabaseScheduler scheduler,
            DatabaseManager manager,
            MinionRepository minions,
            MinionStateRepository states,
            MinionSettingsRepository settings,
            MinionEquipmentRepository equipment,
            MinionStorageRepository storage,
            MinionUpgradeRepository upgrades,
            MinionChestLinkRepository chestLinks,
            MinionPersistenceService persistence
    ) {
        this.logger = logger;
        this.scheduler = scheduler;
        this.manager = manager;
        this.minions = minions;
        this.states = states;
        this.settings = settings;
        this.equipment = equipment;
        this.storage = storage;
        this.upgrades = upgrades;
        this.chestLinks = chestLinks;
        this.persistence = persistence;
    }

    public static MinionDatabase open(Logger logger, File dataFolder, DatabaseSettings settings) {
        DatabaseScheduler scheduler = new DatabaseScheduler("EternalMinions-Database");
        DatabaseManager manager = new DatabaseManager(logger, dataFolder, settings);
        MinionRepository minions = new MinionRepository(manager, scheduler);
        MinionStateRepository states = new MinionStateRepository(manager, scheduler);
        MinionSettingsRepository minionSettings = new MinionSettingsRepository(manager, scheduler);
        MinionEquipmentRepository equipment = new MinionEquipmentRepository(manager, scheduler);
        MinionStorageRepository storage = new MinionStorageRepository(manager, scheduler);
        MinionUpgradeRepository upgrades = new MinionUpgradeRepository(manager, scheduler);
        MinionChestLinkRepository chestLinks = new MinionChestLinkRepository(manager, scheduler);
        MinionActionRepository actions = new MinionActionRepository(manager, scheduler);
        MinionPersistenceService persistence = new MinionPersistenceService(
                logger,
                minions,
                states,
                minionSettings,
                equipment,
                storage,
                upgrades,
                chestLinks,
                actions
        );
        return new MinionDatabase(
                logger,
                scheduler,
                manager,
                minions,
                states,
                minionSettings,
                equipment,
                storage,
                upgrades,
                chestLinks,
                persistence
        );
    }

    public MinionPersistenceService persistence() {
        return this.persistence;
    }

    public CompletableFuture<Void> initialize() {
        return this.scheduler.completeAsync(() -> {
            this.manager.connect();
            return null;
        }).thenCompose(ignored -> this.minions.initialize())
                .thenCompose(ignored -> this.states.initialize())
                .thenCompose(ignored -> this.settings.initialize())
                .thenCompose(ignored -> this.equipment.initialize())
                .thenCompose(ignored -> this.storage.initialize())
                .thenCompose(ignored -> this.upgrades.initialize())
                .thenCompose(ignored -> this.chestLinks.initialize())
                .thenRun(this.minions::markReady);
    }

    public CompletableFuture<List<MinionData>> loadAll() {
        return this.minions.loadAll();
    }

    public void close() {
        try {
            this.scheduler.close();
            this.manager.close();
        }
        catch (Exception exception) {
            this.logger.log(Level.SEVERE, "Unable to close minion database cleanly", exception);
        }
    }
}
