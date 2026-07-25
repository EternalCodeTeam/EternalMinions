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
import com.eternalcode.minions.database.DatabaseManager;
import com.eternalcode.minions.database.DatabaseScheduler;
import com.eternalcode.minions.database.MinionChestLinkRepository;
import com.eternalcode.minions.database.MinionData;
import com.eternalcode.minions.database.MinionEquipmentRepository;
import com.eternalcode.minions.database.MinionPersistenceService;
import com.eternalcode.minions.database.MinionRepository;
import com.eternalcode.minions.database.MinionSettingsRepository;
import com.eternalcode.minions.database.MinionStateRepository;
import com.eternalcode.minions.database.MinionStorageRepository;
import com.eternalcode.minions.database.MinionUpgradeRepository;
import com.eternalcode.minions.gui.MinionPanel;
import com.eternalcode.minions.gui.MinionUpgradePanel;
import com.eternalcode.minions.item.MinionItemFactory;
import com.eternalcode.minions.minion.ChestLinkService;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionActionEngine;
import com.eternalcode.minions.minion.MinionBehavior;
import com.eternalcode.minions.minion.MinionBehaviorType;
import com.eternalcode.minions.minion.MinionIdSequence;
import com.eternalcode.minions.minion.MinionLifecycleService;
import com.eternalcode.minions.minion.MinionPlacementListener;
import com.eternalcode.minions.minion.MinionRegistry;
import com.eternalcode.minions.minion.MinionService;
import com.eternalcode.minions.minion.MinionType;
import com.eternalcode.minions.minion.MinionTypeService;
import com.eternalcode.minions.minion.MinionUpgradeService;
import com.eternalcode.minions.minion.collector.CollectorBehavior;
import com.eternalcode.minions.minion.miner.MiningBehavior;
import com.eternalcode.minions.minion.seller.SellerBehavior;
import com.eternalcode.minions.minion.seller.NoopShopIntegration;
import com.eternalcode.minions.minion.status.CoreMinionStatuses;
import com.eternalcode.minions.minion.status.MinionStatusTracker;
import com.eternalcode.minions.minion.tool.ToolDurabilityService;
import com.eternalcode.minions.minion.tool.ToolValidationService;
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
import com.eternalcode.multification.notice.Notice;
import com.github.retrooper.packetevents.PacketEvents;
import dev.rollczi.litecommands.LiteCommands;
import dev.rollczi.litecommands.adventure.LiteAdventureExtension;
import dev.rollczi.litecommands.bukkit.LiteBukkitFactory;
import dev.rollczi.litecommands.bukkit.LiteBukkitMessages;
import java.io.File;
import java.util.EnumMap;
import java.util.Map;
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
    private DatabaseManager database;
    private MinionInteractionListener interactionListener;
    private DatabaseScheduler databaseScheduler;
    private LiteCommands<CommandSender> liteCommands;
    private BukkitTask runtimeTask;
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
                new MinionTypeService(configs, minionsConfig, this.getServer(), new File(dataFolder, "minions"));

        EntityLib.init(
                new SpigotEntityLibPlatform(this),
                new APIConfig(PacketEvents.getAPI()).usePlatformLogger()
        );

        this.minionRegistry = new MinionRegistry();
        MinionEntityIndex entityIndex = new MinionEntityIndex();
        MinionStatusTracker statusTracker = new MinionStatusTracker(CoreMinionStatuses.IDLE);
        EntityLibHologramRenderer holograms =
                new EntityLibHologramRenderer(
                        this.getServer(),
                        miniMessage,
                        minionTypes,
                        minionsConfig,
                        statusTracker);
        this.renderer = switch (minionsConfig.minionRenderer) {
            case ARMOR_STAND -> new ArmorStandMinionRenderer(holograms, entityIndex, minionTypes);
            case NPC -> new NpcMinionRenderer(holograms, entityIndex, minionTypes);
        };

        MinionRenderService renders =
                new MinionRenderService(this.getServer(), this.minionRegistry, this.renderer, minionsConfig);

        this.databaseScheduler = new DatabaseScheduler("EternalMinions-Database");
        this.database = new DatabaseManager(this.getLogger(), dataFolder, databaseConfig);
        this.minionRepository = new MinionRepository(this.database, this.databaseScheduler);
        MinionPersistenceService persistence = new MinionPersistenceService(
                this.getLogger(),
                this.minionRepository,
                new MinionStateRepository(this.database, this.databaseScheduler),
                new MinionSettingsRepository(this.database, this.databaseScheduler),
                new MinionEquipmentRepository(this.database, this.databaseScheduler),
                new MinionStorageRepository(this.database, this.databaseScheduler),
                new MinionUpgradeRepository(this.database, this.databaseScheduler),
                new MinionChestLinkRepository(this.database, this.databaseScheduler)
        );
        ToolValidationService toolValidation = new ToolValidationService();
        ToolDurabilityService toolDurability = new ToolDurabilityService();
        com.eternalcode.minions.minion.tool.ToolInventoryLocator toolLocator =
                new com.eternalcode.minions.minion.tool.ToolInventoryLocator();
        Map<MinionBehaviorType, MinionBehavior> behaviors = new EnumMap<>(MinionBehaviorType.class);
        behaviors.put(
                MinionBehaviorType.MINER,
                new MiningBehavior(
                        this.minionRegistry,
                        persistence,
                        this.renderer,
                        statusTracker,
                        toolValidation,
                        toolDurability,
                        toolLocator));
        behaviors.put(
                MinionBehaviorType.LUMBERJACK,
                new com.eternalcode.minions.minion.lumberjack.LumberjackBehavior(
                        this.minionRegistry,
                        persistence,
                        this.renderer,
                        statusTracker,
                        toolValidation,
                        toolDurability,
                        toolLocator));
        behaviors.put(
                MinionBehaviorType.FARMER,
                new com.eternalcode.minions.minion.farmer.FarmerBehavior(
                        this.minionRegistry,
                        persistence,
                        this.renderer,
                        statusTracker,
                        toolValidation,
                        toolDurability,
                        toolLocator));
        behaviors.put(
                MinionBehaviorType.FISHERMAN,
                new com.eternalcode.minions.minion.fisherman.FishermanBehavior(
                        this.minionRegistry,
                        persistence,
                        this.renderer,
                        statusTracker,
                        toolValidation,
                        toolDurability,
                        toolLocator));
        com.eternalcode.minions.minion.killer.KillerLootingListener killerLooting =
                new com.eternalcode.minions.minion.killer.KillerLootingListener();
        this.getServer().getPluginManager().registerEvents(killerLooting, this);
        behaviors.put(
                MinionBehaviorType.KILLER,
                new com.eternalcode.minions.minion.killer.KillerBehavior(
                        this.minionRegistry,
                        persistence,
                        this.renderer,
                        statusTracker,
                        toolValidation,
                        toolDurability,
                        toolLocator,
                        killerLooting));
        behaviors.put(
                MinionBehaviorType.CRAFTER,
                new com.eternalcode.minions.minion.crafter.CrafterBehavior(
                        this.minionRegistry,
                        persistence,
                        this.renderer,
                        statusTracker));
        behaviors.put(
                MinionBehaviorType.COLLECTOR,
                new CollectorBehavior(
                        this.minionRegistry,
                        persistence,
                        this.renderer,
                        statusTracker,
                        toolValidation,
                        toolLocator));
        behaviors.put(
                MinionBehaviorType.SELLER,
                new SellerBehavior(
                        this.minionRegistry,
                        persistence,
                        this.renderer,
                        statusTracker,
                        new NoopShopIntegration())
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
                minionItems
        );
        BiConsumer<Player, Minion> pickupHandler = (player, minion) -> {
            lifecycle.pickup(player, minion);
            notices.create().viewer(player).notice(messages.minionPickedUp).send();
            player.closeInventory();
        };
        MinionUpgradeService upgradeService =
                new MinionUpgradeService(minionTypes, lifecycle::updateUpgrade, messages, notices);
        MinionUpgradePanel upgradePanel =
                new MinionUpgradePanel(this, panelConfig, miniMessage, minionTypes, upgradeService);
        ChestLinkService chestLinks =
                new ChestLinkService(this.minionRegistry, minionsConfig, lifecycle::updateChestLink, messages, notices);
        MinionPanel panel = new MinionPanel(
                this,
                panelConfig,
                messages,
                notices,
                miniMessage,
                minionTypes,
                lifecycle,
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
        if (this.databaseScheduler == null) {
            return;
        }

        try {
            this.databaseScheduler.close();
            if (this.database != null) {
                this.database.close();
            }
        }
        catch (Exception exception) {
            this.getLogger().log(Level.SEVERE, "Unable to close minion database cleanly", exception);
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
