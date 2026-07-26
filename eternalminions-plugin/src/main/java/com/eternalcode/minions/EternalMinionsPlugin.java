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
import com.eternalcode.minions.item.MinionAppearanceItems;
import com.eternalcode.minions.minion.storage.ChestLinkService;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionActionEngine;
import com.eternalcode.minions.minion.MinionBehavior;
import com.eternalcode.minions.minion.MinionBehaviorRegistry;
import com.eternalcode.minions.minion.MinionIdSequence;
import com.eternalcode.minions.minion.MinionLifecycleService;
import com.eternalcode.minions.minion.MinionPlacementListener;
import com.eternalcode.minions.minion.MinionRegistry;
import com.eternalcode.minions.minion.MinionService;
import com.eternalcode.minions.minion.upgrade.MinionUpgradeService;
import com.eternalcode.minions.minion.impl.collector.CollectorBehavior;
import com.eternalcode.minions.minion.impl.collector.CollectorConfig;
import com.eternalcode.minions.minion.impl.crafter.CrafterBehavior;
import com.eternalcode.minions.minion.impl.crafter.CrafterConfig;
import com.eternalcode.minions.minion.impl.farmer.FarmerBehavior;
import com.eternalcode.minions.minion.impl.farmer.FarmerConfig;
import com.eternalcode.minions.minion.impl.fisherman.FishermanBehavior;
import com.eternalcode.minions.minion.impl.fisherman.FishermanConfig;
import com.eternalcode.minions.minion.impl.killer.KillerBehavior;
import com.eternalcode.minions.minion.impl.killer.KillerConfig;
import com.eternalcode.minions.minion.impl.killer.KillerLootingListener;
import com.eternalcode.minions.minion.impl.lumberjack.LumberjackBehavior;
import com.eternalcode.minions.minion.impl.lumberjack.LumberjackConfig;
import com.eternalcode.minions.minion.impl.miner.MinerConfig;
import com.eternalcode.minions.minion.impl.miner.MiningBehavior;
import com.eternalcode.minions.minion.impl.seller.NoopShopIntegration;
import com.eternalcode.minions.minion.impl.seller.SellerBehavior;
import com.eternalcode.minions.minion.impl.seller.SellerConfig;
import com.eternalcode.minions.minion.impl.seller.ShopIntegration;
import com.eternalcode.minions.minion.status.CoreMinionStatuses;
import com.eternalcode.minions.minion.status.MinionStatusTracker;
import com.eternalcode.minions.minion.tool.ToolDurabilityService;
import com.eternalcode.minions.minion.tool.ToolInventoryLocator;
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
import java.util.List;
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
        File minionConfigDirectory = new File(dataFolder, "minions");
        minionConfigDirectory.mkdirs();
        MinionBehaviorRegistry behaviors = new MinionBehaviorRegistry();
        ToolValidationService toolValidation = new ToolValidationService();
        ToolDurabilityService toolDurability = new ToolDurabilityService();
        ToolInventoryLocator toolLocator = new ToolInventoryLocator();
        KillerLootingListener killerLooting = new KillerLootingListener();
        ShopIntegration shop = new NoopShopIntegration();
        this.getServer().getPluginManager().registerEvents(killerLooting, this);
        behaviors.replace(this.createBehaviors(
            configs,
            minionConfigDirectory,
            toolValidation,
            toolDurability,
            toolLocator,
            killerLooting,
            shop
        ));
        MinionAppearanceItems appearance = new MinionAppearanceItems(this.getServer());

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
                        behaviors,
                        minionsConfig,
                        statusTracker);
        this.renderer = switch (minionsConfig.minionRenderer) {
            case ARMOR_STAND -> new ArmorStandMinionRenderer(holograms, entityIndex, behaviors, appearance);
            case NPC -> new NpcMinionRenderer(holograms, entityIndex, behaviors, appearance);
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
        MinionActionEngine actions = new MinionActionEngine(
                this.getServer(),
                this.minionRegistry,
                minionsConfig,
                behaviors,
                persistence,
                statusTracker,
                this.renderer
        );

        MinionItemFactory minionItems = new MinionItemFactory(this, behaviors, appearance, miniMessage);
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
                new MinionUpgradeService(behaviors, lifecycle::updateUpgrade, messages, notices);
        MinionUpgradePanel upgradePanel =
                new MinionUpgradePanel(this, panelConfig, miniMessage, behaviors, upgradeService);
        ChestLinkService chestLinks =
                new ChestLinkService(this.minionRegistry, minionsConfig, lifecycle::updateChestLink, messages, notices);
        MinionPanel panel = new MinionPanel(
                this,
                panelConfig,
                messages,
                notices,
                miniMessage,
                behaviors,
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
                        behaviors,
                        messages,
                        notices),
                this
        );

        Runnable reloadAll = () -> {
            configs.reload();
            behaviors.replace(this.createBehaviors(
                configs,
                minionConfigDirectory,
                toolValidation,
                toolDurability,
                toolLocator,
                killerLooting,
                shop
            ));
        };
        this.liteCommands = LiteBukkitFactory.builder("eternalminions", this, this.getServer())
                .commands(
                        new ReloadCommand(reloadAll, messages),
                        new MinionGiveCommand(minionItems, behaviors, messages))
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
                                    MinionBehavior behavior = behaviors.find(minion.behaviorId()).orElse(null);
                                    if (behavior != null && behavior.storageCapacity(minion) > minion.storage()
                                            .capacity()) {
                                        minion = minion.withStorage(
                                                minion.storage().resized(behavior.storageCapacity(minion)));
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

    private List<MinionBehavior> createBehaviors(
        ConfigService configs,
        File directory,
        ToolValidationService toolValidation,
        ToolDurabilityService toolDurability,
        ToolInventoryLocator toolLocator,
        KillerLootingListener killerLooting,
        ShopIntegration shop
    ) {
        return List.of(
            this.createMiner(configs, directory, toolValidation, toolDurability, toolLocator),
            this.createLumberjack(configs, directory, toolValidation, toolDurability, toolLocator),
            this.createFarmer(configs, directory, toolValidation, toolDurability, toolLocator),
            this.createFisherman(configs, directory, toolValidation, toolDurability, toolLocator),
            this.createKiller(
                configs,
                directory,
                toolValidation,
                toolDurability,
                toolLocator,
                killerLooting
            ),
            this.createCollector(configs, directory, toolValidation, toolLocator),
            this.createCrafter(configs, directory),
            this.createSeller(configs, directory, shop)
        );
    }

    private MinionBehavior createMiner(
        ConfigService configs,
        File directory,
        ToolValidationService toolValidation,
        ToolDurabilityService toolDurability,
        ToolInventoryLocator toolLocator
    ) {
        MinerConfig config = configs.load(MinerConfig.class, new File(directory, "miner.yml"));
        return new MiningBehavior(config, toolValidation, toolDurability, toolLocator);
    }

    private MinionBehavior createLumberjack(
        ConfigService configs,
        File directory,
        ToolValidationService toolValidation,
        ToolDurabilityService toolDurability,
        ToolInventoryLocator toolLocator
    ) {
        LumberjackConfig config = configs.load(LumberjackConfig.class, new File(directory, "lumberjack.yml"));
        return new LumberjackBehavior(config, toolValidation, toolDurability, toolLocator);
    }

    private MinionBehavior createFarmer(
        ConfigService configs,
        File directory,
        ToolValidationService toolValidation,
        ToolDurabilityService toolDurability,
        ToolInventoryLocator toolLocator
    ) {
        FarmerConfig config = configs.load(FarmerConfig.class, new File(directory, "farmer.yml"));
        return new FarmerBehavior(config, toolValidation, toolDurability, toolLocator);
    }

    private MinionBehavior createFisherman(
        ConfigService configs,
        File directory,
        ToolValidationService toolValidation,
        ToolDurabilityService toolDurability,
        ToolInventoryLocator toolLocator
    ) {
        FishermanConfig config = configs.load(FishermanConfig.class, new File(directory, "fisherman.yml"));
        return new FishermanBehavior(config, this.getServer(), toolValidation, toolDurability, toolLocator);
    }

    private MinionBehavior createKiller(
        ConfigService configs,
        File directory,
        ToolValidationService toolValidation,
        ToolDurabilityService toolDurability,
        ToolInventoryLocator toolLocator,
        KillerLootingListener killerLooting
    ) {
        KillerConfig config = configs.load(KillerConfig.class, new File(directory, "killer.yml"));
        return new KillerBehavior(
            config,
            toolValidation,
            toolDurability,
            toolLocator,
            killerLooting
        );
    }

    private MinionBehavior createCollector(
        ConfigService configs,
        File directory,
        ToolValidationService toolValidation,
        ToolInventoryLocator toolLocator
    ) {
        CollectorConfig config = configs.load(CollectorConfig.class, new File(directory, "collector.yml"));
        return new CollectorBehavior(config, toolValidation, toolLocator);
    }

    private MinionBehavior createCrafter(ConfigService configs, File directory) {
        CrafterConfig config = configs.load(CrafterConfig.class, new File(directory, "crafter.yml"));
        return new CrafterBehavior(config);
    }

    private MinionBehavior createSeller(ConfigService configs, File directory, ShopIntegration shop) {
        SellerConfig config = configs.load(SellerConfig.class, new File(directory, "seller.yml"));
        return new SellerBehavior(config, shop);
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
