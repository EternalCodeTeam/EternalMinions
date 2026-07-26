package com.eternalcode.minions.database;

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
    private final MinionPersistenceService persistence;

    private MinionDatabase(
            Logger logger,
            DatabaseScheduler scheduler,
            DatabaseManager manager,
            MinionRepository minions,
            MinionPersistenceService persistence
    ) {
        this.logger = logger;
        this.scheduler = scheduler;
        this.manager = manager;
        this.minions = minions;
        this.persistence = persistence;
    }

    public static MinionDatabase open(Logger logger, File dataFolder, DatabaseSettings settings) {
        DatabaseScheduler scheduler = new DatabaseScheduler("EternalMinions-Database");
        DatabaseManager manager = new DatabaseManager(logger, dataFolder, settings);
        MinionRepository minions = new MinionRepository(manager, scheduler);
        MinionPersistenceService persistence = new MinionPersistenceService(
                logger,
                minions,
                new MinionStateRepository(manager, scheduler),
                new MinionSettingsRepository(manager, scheduler),
                new MinionEquipmentRepository(manager, scheduler),
                new MinionStorageRepository(manager, scheduler),
                new MinionUpgradeRepository(manager, scheduler),
                new MinionChestLinkRepository(manager, scheduler)
        );
        return new MinionDatabase(logger, scheduler, manager, minions, persistence);
    }

    public MinionPersistenceService persistence() {
        return this.persistence;
    }

    public CompletableFuture<Void> initialize() {
        return this.minions.initialize();
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
