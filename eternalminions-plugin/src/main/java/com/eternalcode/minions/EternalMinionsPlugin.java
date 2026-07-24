package com.eternalcode.minions;

import com.eternalcode.minions.command.InvalidUsageHandler;
import com.eternalcode.minions.command.MinionGiveCommand;
import com.eternalcode.minions.command.MissingPermissionHandler;
import com.eternalcode.minions.command.ReloadCommand;
import com.eternalcode.minions.config.ConfigService;
import com.eternalcode.minions.config.MessagesConfig;
import com.eternalcode.minions.config.MinionPanelConfig;
import com.eternalcode.minions.config.MinionsConfig;
import com.eternalcode.minions.database.DatabaseConfig;
import com.eternalcode.minions.database.MinionData;
import com.eternalcode.minions.database.MinionDatabase;
import com.eternalcode.minions.database.MinionPersistenceService;
import com.eternalcode.minions.database.MinionRepository;
import com.eternalcode.minions.gui.MinionPanel;
import com.eternalcode.minions.gui.MinionUpgradePanel;
import com.eternalcode.minions.integration.VaultEconomyHook;
import com.eternalcode.minions.item.MinionItemFactory;
import com.eternalcode.minions.minion.ChestLinkService;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionBehaviorType;
import com.eternalcode.minions.minion.MinionIdSequence;
import com.eternalcode.minions.minion.MinionLifecycleService;
import com.eternalcode.minions.minion.MinionPlacementListener;
import com.eternalcode.minions.minion.MinionRegistry;
import com.eternalcode.minions.minion.MinionService;
import com.eternalcode.minions.minion.MinionType;
import com.eternalcode.minions.minion.MinionTypeService;
import com.eternalcode.minions.minion.MinionUpgradeService;
import com.eternalcode.minions.notice.NoticeResultHandler;
import com.eternalcode.minions.notice.NoticeService;
import com.eternalcode.minions.render.ArmorStandMinionRenderer;
import com.eternalcode.minions.render.EntityLibHologramRenderer;
import com.eternalcode.minions.render.MinionEntityIndex;
import com.eternalcode.minions.render.MinionInteractionListener;
import com.eternalcode.minions.render.MinionRenderService;
import com.eternalcode.minions.render.MinionRenderer;
import com.eternalcode.minions.render.MinionViewerListener;
import com.eternalcode.minions.render.NpcMinionRenderer;
import com.eternalcode.minions.scheduler.CollectorBehavior;
import com.eternalcode.minions.scheduler.GeneratorBehavior;
import com.eternalcode.minions.scheduler.MiningBehavior;
import com.eternalcode.minions.scheduler.MinionActionEngine;
import com.eternalcode.minions.scheduler.MinionBehavior;
import com.eternalcode.minions.scheduler.SellerBehavior;
import com.eternalcode.multification.notice.Notice;
import com.github.retrooper.packetevents.PacketEvents;
import dev.rollczi.litecommands.LiteCommands;
import dev.rollczi.litecommands.adventure.LiteAdventureExtension;
import dev.rollczi.litecommands.bukkit.LiteBukkitFactory;
import dev.rollczi.litecommands.bukkit.LiteBukkitMessages;
import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import me.tofaa.entitylib.APIConfig;
import me.tofaa.entitylib.EntityLib;
import me.tofaa.entitylib.spigot.SpigotEntityLibPlatform;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class EternalMinionsPlugin extends JavaPlugin implements EternalMinionsApi {

    private MinionRegistry minionRegistry;
    private MinionRenderer renderer;
    private MinionRepository minionRepository;
    private MinionDatabase database;
    private MinionInteractionListener interactionListener;
    private ExecutorService databaseExecutor;
    private LiteCommands<CommandSender> liteCommands;
    private BukkitTask runtimeTask;
    private BukkitTask persistenceTask;
    private boolean databaseReady;
    private boolean apiInitialized;

    @Override
    public void onEnable() {
        File dataFolder = this.getDataFolder();
        dataFolder.mkdirs();

        ConfigService configs = new ConfigService();
        MinionsConfig minionsConfig = configs.create(MinionsConfig.class, new File(dataFolder, "config.yml"));
        MessagesConfig messages = configs.create(MessagesConfig.class, new File(dataFolder, "messages.yml"));
        MinionPanelConfig panelConfig = configs.create(MinionPanelConfig.class, new File(dataFolder, "panel.yml"));
        DatabaseConfig databaseConfig = configs.create(DatabaseConfig.class, new File(dataFolder, "database.yml"));

        MiniMessage miniMessage = MiniMessage.miniMessage();
        NoticeService notices = new NoticeService(messages, miniMessage);
        MinionTypeService minionTypes =
                new MinionTypeService(configs, this.getServer(), new File(dataFolder, "minions"));

        EntityLib.init(
                new SpigotEntityLibPlatform(this),
                new APIConfig(PacketEvents.getAPI()).usePlatformLogger()
        );

        this.minionRegistry = new MinionRegistry();
        MinionEntityIndex entityIndex = new MinionEntityIndex();
        EntityLibHologramRenderer holograms =
                new EntityLibHologramRenderer(this.getServer(), miniMessage, minionTypes, minionsConfig);
        this.renderer = switch (minionsConfig.minionRenderer) {
            case ARMOR_STAND -> new ArmorStandMinionRenderer(holograms, entityIndex, minionTypes);
            case NPC -> new NpcMinionRenderer(holograms, entityIndex, minionTypes);
        };

        MinionRenderService renders =
                new MinionRenderService(this.getServer(), this.minionRegistry, this.renderer, minionsConfig);

        this.databaseExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "EternalMinions-Database");
            thread.setDaemon(true);
            return thread;
        });
        this.database = new MinionDatabase(databaseConfig, dataFolder);
        this.minionRepository = new MinionRepository(this.database, this.databaseExecutor);
        MinionPersistenceService persistence =
                new MinionPersistenceService(this, this.minionRegistry, this.minionRepository);
        VaultEconomyHook economy = new VaultEconomyHook(this.getServer());
        MinionBehavior generatorBehavior = new GeneratorBehavior(this.minionRegistry, persistence, this.renderer);
        Map<MinionBehaviorType, MinionBehavior> behaviors = new EnumMap<>(MinionBehaviorType.class);
        behaviors.put(MinionBehaviorType.MINER, new MiningBehavior(this.minionRegistry, persistence, this.renderer));
        behaviors.put(MinionBehaviorType.LUMBERJACK, generatorBehavior);
        behaviors.put(MinionBehaviorType.FARMER, generatorBehavior);
        behaviors.put(MinionBehaviorType.FISHERMAN, generatorBehavior);
        behaviors.put(MinionBehaviorType.KILLER, generatorBehavior);
        behaviors.put(MinionBehaviorType.CRAFTER, generatorBehavior);
        behaviors.put(
                MinionBehaviorType.COLLECTOR,
                new CollectorBehavior(this.minionRegistry, persistence, this.renderer));
        behaviors.put(
                MinionBehaviorType.SELLER,
                new SellerBehavior(this.minionRegistry, persistence, this.renderer, this.getServer(), economy)
        );
        MinionActionEngine actions = new MinionActionEngine(
                this.getServer(),
                this.minionRegistry,
                minionsConfig,
                minionTypes,
                behaviors
        );

        MinionItemFactory minionItems = new MinionItemFactory(this, minionTypes, miniMessage);
        MinionLifecycleService lifecycle = new MinionLifecycleService(
                this.minionRegistry,
                actions,
                renders,
                persistence,
                this.minionRepository,
                minionItems
        );
        BiConsumer<Player, Minion> pickupHandler = (player, minion) -> {
            lifecycle.pickup(player, minion);
            notices.create().viewer(player).notice(messages.minionPickedUp).send();
            player.closeInventory();
        };
        MinionUpgradeService upgradeService =
                new MinionUpgradeService(minionTypes, lifecycle::updateNow, messages, notices);
        MinionUpgradePanel upgradePanel =
                new MinionUpgradePanel(this, panelConfig, miniMessage, minionTypes, upgradeService);
        ChestLinkService chestLinks =
                new ChestLinkService(this.minionRegistry, minionsConfig, lifecycle::updateNow, messages, notices);
        MinionPanel panel = new MinionPanel(
                this,
                panelConfig,
                messages,
                notices,
                miniMessage,
                minionTypes,
                lifecycle::update,
                pickupHandler,
                upgradePanel::open,
                chestLinks::toggle
        );

        this.interactionListener = new MinionInteractionListener(
                this, entityIndex, this.minionRegistry, panel, messages, notices, pickupHandler
        );
        PacketEvents.getAPI().getEventManager().registerListener(this.interactionListener);
        this.getServer().getPluginManager().registerEvents(new MinionViewerListener(renders), this);
        this.getServer().getPluginManager().registerEvents(chestLinks, this);
        this.getServer().getPluginManager().registerEvents(
                new MinionPlacementListener(
                        minionItems,
                        new MinionIdSequence(),
                        lifecycle,
                        minionTypes,
                        messages,
                        notices),
                this
        );

        Runnable reloadAll = () -> {
            configs.reload();
            minionTypes.rebuild();
        };
        this.liteCommands = LiteBukkitFactory.builder("eternalminions", this, this.getServer())
                .commands(
                        new ReloadCommand(reloadAll, messages),
                        new MinionGiveCommand(minionItems, minionTypes, messages))
                .result(Notice.class, new NoticeResultHandler(notices))
                .message(LiteBukkitMessages.PLAYER_NOT_FOUND, messages.playerNotFound)
                .message(LiteBukkitMessages.PLAYER_ONLY, messages.playerOnly)
                .invalidUsage(new InvalidUsageHandler(notices, messages))
                .missingPermission(new MissingPermissionHandler(notices, messages))
                .extension(new LiteAdventureExtension<>())
                .build();

        this.runtimeTask = this.getServer().getScheduler().runTaskTimer(
                this, () -> {
                    actions.run();
                    this.renderer.tick(this.getServer().getCurrentTick());
                }, 1L, 1L);
        this.persistenceTask = this.getServer().getScheduler().runTaskTimer(
                this,
                persistence::flushDirty,
                databaseConfig.dirtyMinionFlushTicks,
                databaseConfig.dirtyMinionFlushTicks
        );

        this.minionRepository.initialize()
                .thenCompose(ignored -> this.minionRepository.loadAll())
                .whenComplete((loaded, error) -> {
                    if (error != null) {
                        this.getLogger()
                                .log(
                                        Level.SEVERE,
                                        "Database initialization failed; minions will run in memory only",
                                        error);
                        return;
                    }
                    if (!this.isEnabled()) {
                        return;
                    }
                    this.getServer().getScheduler().runTask(
                            this, () -> {
                                this.databaseReady = true;
                                for (MinionData data : loaded) {
                                    Minion minion = data.restore();
                                    // Storage rows do not record capacity, so restore the upgraded size from the type.
                                    MinionType type = minionTypes.type(minion.behaviorId()).orElse(null);
                                    if (type != null && type.storageCapacity(minion.upgrades()) > minion.storage()
                                            .capacity()) {
                                        minion = minion.withStorage(
                                                minion.storage().resized(type.storageCapacity(minion.upgrades())));
                                    }
                                    this.minionRegistry.register(minion);
                                    actions.add(minion);
                                    renders.showToNearby(minion);
                                }
                                this.getLogger().info("Loaded " + loaded.size() + " minions from database.");
                            });
                });

        EternalMinionsProvider.initialize(this);
        this.apiInitialized = true;
        this.getLogger().info("EternalMinions initialized with renderer " + minionsConfig.minionRenderer + ".");
    }

    @Override
    public void onDisable() {
        if (this.runtimeTask != null) {
            this.runtimeTask.cancel();
        }
        if (this.persistenceTask != null) {
            this.persistenceTask.cancel();
        }
        if (this.liteCommands != null) {
            this.liteCommands.unregister();
        }
        if (this.interactionListener != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(this.interactionListener);
        }

        if (this.minionRegistry != null && this.renderer != null) {
            for (Minion minion : this.minionRegistry.minions()) {
                this.renderer.remove(minion.id());
            }
        }

        this.flushAndCloseDatabase();

        if (this.apiInitialized) {
            EternalMinionsProvider.deinitialize();
            this.apiInitialized = false;
        }
    }

    private void flushAndCloseDatabase() {
        if (this.databaseExecutor == null) {
            return;
        }

        try {
            if (this.databaseReady) {
                List<MinionData> minions = new ArrayList<>();
                for (Minion minion : this.minionRegistry.minions()) {
                    minions.add(MinionData.capture(minion));
                }
                this.minionRepository.save(minions).get(5L, TimeUnit.SECONDS);
            }
            if (this.database != null) {
                this.database.close();
            }
        }
        catch (Exception exception) {
            this.getLogger().log(Level.SEVERE, "Unable to close minion database cleanly", exception);
        } finally {
            this.databaseExecutor.shutdownNow();
        }
    }

    @Override
    public MinionService minionService() {
        if (this.minionRegistry == null) {
            throw new IllegalStateException("EternalMinions runtime has not been initialized yet!");
        }
        return this.minionRegistry;
    }
}
