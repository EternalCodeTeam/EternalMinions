package com.eternalcode.minions;

import com.eternalcode.minions.minion.access.MinionAccessGuard;
import com.eternalcode.minions.access.MinionAccessService;
import com.eternalcode.minions.minion.access.MinionAccessServiceImpl;
import com.eternalcode.minions.command.InvalidUsageHandler;
import com.eternalcode.minions.command.MinionGiveCommand;
import com.eternalcode.minions.command.MissingPermissionHandler;
import com.eternalcode.minions.command.ReloadCommand;
import com.eternalcode.minions.config.ConfigService;
import com.eternalcode.minions.config.MessagesConfig;
import com.eternalcode.minions.config.MinionPanelConfig;
import com.eternalcode.minions.config.MinionsConfig;
import com.eternalcode.minions.database.DatabaseConfig;
import com.eternalcode.minions.database.MinionDatabase;
import com.eternalcode.minions.database.MinionPersistenceService;
import com.eternalcode.minions.gui.MinionPanel;
import com.eternalcode.minions.gui.MinionUpgradePanel;
import com.eternalcode.minions.item.MinionItemFactory;
import com.eternalcode.minions.item.MinionAppearanceItems;
import com.eternalcode.minions.minion.storage.ChestLinkService;
import com.eternalcode.minions.minion.storage.MinionItemTransferService;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionActionEngine;
import com.eternalcode.minions.minion.MinionBehavior;
import com.eternalcode.minions.minion.MinionBehaviorRegistry;
import com.eternalcode.minions.minion.activity.MinionActivityBypass;
import com.eternalcode.minions.minion.activity.rule.MinionActivityRule;
import com.eternalcode.minions.minion.activity.MinionActivityService;
import com.eternalcode.minions.minion.activity.rule.loadedchunk.LoadedChunkActivityRule;
import com.eternalcode.minions.minion.activity.rule.offline.OfflineActivityRule;
import com.eternalcode.minions.minion.activity.rule.proximity.ProximityActivityRule;
import com.eternalcode.minions.minion.MinionIdSequence;
import com.eternalcode.minions.minion.MinionLifecycleService;
import com.eternalcode.minions.minion.MinionPlacementListener;
import com.eternalcode.minions.minion.MinionPistonProtectionListener;
import com.eternalcode.minions.minion.MinionRegistry;
import com.eternalcode.minions.minion.MinionService;
import com.eternalcode.minions.minion.upgrade.MinionUpgradeService;
import com.eternalcode.minions.minion.impl.collector.CollectorBehavior;
import com.eternalcode.minions.minion.impl.crafter.CrafterBehavior;
import com.eternalcode.minions.minion.impl.farmer.FarmerBehavior;
import com.eternalcode.minions.minion.impl.fisherman.FishermanBehavior;
import com.eternalcode.minions.minion.impl.killer.KillerBehavior;
import com.eternalcode.minions.minion.impl.killer.KillerLootingListener;
import com.eternalcode.minions.minion.impl.lumberjack.LumberjackBehavior;
import com.eternalcode.minions.minion.impl.miner.MiningBehavior;
import com.eternalcode.minions.minion.impl.seller.SellerBehavior;
import com.eternalcode.minions.minion.impl.seller.SellerConfig;
import com.eternalcode.minions.minion.status.CoreMinionStatuses;
import com.eternalcode.minions.minion.status.MinionStatusTracker;
import com.eternalcode.minions.minion.tool.ToolDurabilityService;
import com.eternalcode.minions.minion.tool.ToolInventoryLocator;
import com.eternalcode.minions.minion.tool.MinionToolService;
import com.eternalcode.minions.minion.tool.ToolValidationService;
import com.eternalcode.minions.notice.NoticeResultHandler;
import com.eternalcode.minions.notice.NoticeService;
import com.eternalcode.minions.render.MinionEntityIndex;
import com.eternalcode.minions.render.MinionInteractionListener;
import com.eternalcode.minions.render.MinionRenderService;
import com.eternalcode.minions.render.MinionRenderer;
import com.eternalcode.minions.render.MinionViewerListener;
import com.eternalcode.minions.bridge.BridgeManager;
import com.eternalcode.minions.bridge.shop.MinionShopServiceImpl;
import com.eternalcode.minions.bridge.shop.ShopBridges;
import com.eternalcode.minions.bridge.vault.VaultBridge;
import com.eternalcode.minions.bridge.vault.VaultEconomyHook;
import com.eternalcode.minions.minion.limit.MinionLimitStatus;
import com.eternalcode.minions.minion.limit.PlayerMinionLimitService;
import com.eternalcode.minions.shop.MinionShopProvider;
import com.eternalcode.minions.shop.MinionShopService;
import com.eternalcode.multification.notice.Notice;
import com.github.retrooper.packetevents.PacketEvents;
import dev.rollczi.litecommands.LiteCommands;
import dev.rollczi.litecommands.adventure.LiteAdventureExtension;
import dev.rollczi.litecommands.argument.ArgumentKey;
import dev.rollczi.litecommands.bukkit.LiteBukkitFactory;
import dev.rollczi.litecommands.bukkit.LiteBukkitMessages;
import dev.rollczi.litecommands.suggestion.SuggestionResult;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class EternalMinionsPlugin extends JavaPlugin implements EternalMinionsApi {

    private MinionRegistry minionRegistry;
    private MinionAccessServiceImpl minionAccess;
    private MinionShopServiceImpl shopService;
    private MinionRenderer renderer;
    private MinionDatabase database;
    private MinionInteractionListener interactionListener;
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
        MinionItemTransferService itemTransfers = new MinionItemTransferService(minionsConfig);
        MinionToolService tools = new MinionToolService(
                toolValidation,
                toolDurability,
                toolLocator,
                itemTransfers
        );
        KillerLootingListener killerLooting = new KillerLootingListener();
        SellerConfig sellerConfig = configs.load(SellerConfig.class, new File(minionConfigDirectory, "seller.yml"));
        BridgeManager bridgeManager = new BridgeManager(this.getLogger());
        Optional<VaultEconomyHook> economy = VaultBridge.discover(bridgeManager, this);
        List<MinionShopProvider> shopHooks =
                ShopBridges.discover(bridgeManager, this, sellerConfig.sellPrices, economy);
        this.shopService = new MinionShopServiceImpl(this.getLogger(), shopHooks);
        this.getServer().getPluginManager().registerEvents(this.shopService, this);
        this.getServer().getPluginManager().registerEvents(killerLooting, this);
        behaviors.replace(this.createBehaviors(
            configs,
            minionConfigDirectory,
            tools,
            itemTransfers,
            killerLooting,
            this.shopService
        ));
        MinionAppearanceItems appearance = new MinionAppearanceItems(this.getServer());

        this.minionRegistry = new MinionRegistry();
        PlayerMinionLimitService playerLimits = new PlayerMinionLimitService(this.minionRegistry, minionsConfig.limits);
        this.minionAccess = new MinionAccessServiceImpl(this.getLogger());
        MinionAccessGuard access = new MinionAccessGuard(
                this.minionRegistry,
                this.minionAccess,
                messages,
                notices
        );
        MinionEntityIndex entityIndex = new MinionEntityIndex();
        MinionStatusTracker statusTracker = new MinionStatusTracker(CoreMinionStatuses.IDLE);
        this.renderer = MinionRenderer.create(
                this,
                minionsConfig,
                miniMessage,
                behaviors,
                statusTracker,
                entityIndex,
                appearance
        );

        MinionRenderService renders = new MinionRenderService(
                this.getServer(),
                this.minionRegistry,
                this.renderer,
                minionsConfig
        );

        this.database = MinionDatabase.open(this.getLogger(), dataFolder, databaseConfig);
        MinionPersistenceService persistence = this.database.persistence();
        MinionActivityBypass activityBypass = new MinionActivityBypass(minionsConfig.activity.bypass);
        List<MinionActivityRule> activityRules = List.of(
                new LoadedChunkActivityRule(minionsConfig.activity.loadedChunk),
                new OfflineActivityRule(minionsConfig.activity.offline),
                new ProximityActivityRule(minionsConfig.activity.proximity)
        );
        MinionActivityService activityService =
                new MinionActivityService(this.getServer(), activityBypass, activityRules);
        MinionActionEngine actions = new MinionActionEngine(
                this.getServer(),
                this.minionRegistry,
                minionsConfig,
                behaviors,
                persistence,
                statusTracker,
                this.renderer,
                activityService
        );

        MinionItemFactory minionItems = new MinionItemFactory(this, behaviors, appearance, miniMessage);
        MinionLifecycleService lifecycle = new MinionLifecycleService(
                this.minionRegistry,
                actions,
                renders,
                persistence,
                minionItems,
                behaviors,
                access,
                itemTransfers
        );
        BiConsumer<Player, Minion> pickupHandler = (player, minion) -> {
            if (!lifecycle.pickup(player, minion)) {
                return;
            }

            MinionLimitStatus limitStatus = playerLimits.statusFor(player);
            notices.create().viewer(player).notice(messages.minionPickedUp)
                    .placeholder("{MINION_LIMIT_CURRENT}", Integer.toString(limitStatus.current()))
                    .placeholder("{MINION_LIMIT_MAX}", limitStatus.maxDisplay())
                    .send();
            player.closeInventory();
        };
        MinionUpgradeService upgradeService =
                new MinionUpgradeService(
                        behaviors,
                        access,
                        lifecycle::updateUpgrade,
                        messages,
                        notices,
                        economy
                );
        MinionUpgradePanel upgradePanel =
                new MinionUpgradePanel(
                        this,
                        panelConfig,
                        miniMessage,
                        behaviors,
                        upgradeService,
                        access
                );
        ChestLinkService chestLinks =
                new ChestLinkService(
                        access,
                        minionsConfig,
                        lifecycle::updateChestLink,
                        messages,
                        notices
                );
        MinionPanel panel = new MinionPanel(
                this,
                panelConfig,
                messages,
                notices,
                miniMessage,
                behaviors,
                lifecycle,
                access,
                itemTransfers,
                pickupHandler,
                upgradePanel::open,
                chestLinks::toggle
        );

        this.interactionListener = new MinionInteractionListener(
                this,
                entityIndex,
                access,
                panel,
                pickupHandler
        );
        PacketEvents.getAPI().getEventManager().registerListener(this.interactionListener);
        this.getServer().getPluginManager().registerEvents(this.minionAccess, this);
        this.getServer().getPluginManager().registerEvents(new MinionViewerListener(renders), this);
        this.getServer().getPluginManager().registerEvents(
                new MinionPistonProtectionListener(this.minionRegistry),
                this
        );
        this.getServer().getPluginManager().registerEvents(chestLinks, this);
        this.getServer().getPluginManager().registerEvents(
                new MinionPlacementListener(
                        minionItems,
                        new MinionIdSequence(),
                        lifecycle,
                        behaviors,
                        messages,
                        notices,
                        playerLimits),
                this
        );

        Runnable reloadAll = () -> {
            configs.reload();
            behaviors.replace(this.createBehaviors(
                configs,
                minionConfigDirectory,
                tools,
                itemTransfers,
                killerLooting,
                this.shopService
            ));
        };
        this.liteCommands = LiteBukkitFactory.builder("eternalminions", this, this.getServer())
                .argumentSuggestion(
                        String.class,
                        ArgumentKey.of("type"),
                        SuggestionResult.of(behaviors.ids()))
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

        this.database.initialize()
                .thenCompose(ignored -> this.database.loadAll())
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
                                int restored = lifecycle.restoreAll(loaded);
                                this.getLogger().info("Loaded " + restored + " minions from database.");
                            });
                });

        EternalMinionsProvider.initialize(this);
        this.apiInitialized = true;
        this.getLogger().info("EternalMinions initialized with renderer " + minionsConfig.minionRenderer + ".");
    }

    private List<MinionBehavior> createBehaviors(
        ConfigService configs,
        File directory,
        MinionToolService tools,
        MinionItemTransferService transfers,
        KillerLootingListener killerLooting,
        MinionShopProvider shop
    ) {
        List<MinionBehavior> behaviors = List.of(
            MiningBehavior.create(configs, directory, tools, transfers),
            LumberjackBehavior.create(configs, directory, tools, transfers),
            FarmerBehavior.create(configs, directory, tools, transfers),
            FishermanBehavior.create(configs, directory, tools, transfers),
            KillerBehavior.create(configs, directory, tools, killerLooting),
            CollectorBehavior.create(configs, directory, tools, transfers),
            CrafterBehavior.create(configs, directory, transfers),
            SellerBehavior.create(configs, directory, shop)
        );

        return behaviors.stream()
            .filter(behavior -> behavior.config().enabled)
            .toList();
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

        if (this.database != null) {
            this.database.close();
        }

        if (this.apiInitialized) {
            EternalMinionsProvider.deinitialize();
            this.apiInitialized = false;
        }
    }

    @Override
    public MinionService minionService() {
        if (this.minionRegistry == null) {
            throw new IllegalStateException("EternalMinions runtime has not been initialized yet!");
        }
        return this.minionRegistry;
    }

    @Override
    public MinionAccessService minionAccessService() {
        if (this.minionAccess == null) {
            throw new IllegalStateException(
                    "EternalMinions runtime has not been initialized yet!"
            );
        }

        return this.minionAccess;
    }

    @Override
    public MinionShopService minionShopService() {
        if (this.shopService == null) {
            throw new IllegalStateException(
                    "EternalMinions runtime has not been initialized yet!"
            );
        }

        return this.shopService;
    }
}
