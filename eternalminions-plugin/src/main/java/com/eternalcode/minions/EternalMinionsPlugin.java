package com.eternalcode.minions;

import com.eternalcode.minions.bridge.BridgeManager;
import com.eternalcode.minions.bridge.shop.MinionShopServiceImpl;
import com.eternalcode.minions.bridge.shop.ShopBridges;
import com.eternalcode.minions.bridge.vault.VaultBridge;
import com.eternalcode.minions.bridge.vault.VaultEconomyHook;
import com.eternalcode.minions.command.handler.InvalidUsageHandlerImpl;
import com.eternalcode.minions.command.MinionGiveCommand;
import com.eternalcode.minions.command.handler.MissingPermissionHandlerImpl;
import com.eternalcode.minions.command.ReloadCommand;
import com.eternalcode.minions.config.ConfigService;
import com.eternalcode.minions.config.MessagesConfig;
import com.eternalcode.minions.config.MinionPanelConfig;
import com.eternalcode.minions.config.MinionsConfig;
import com.eternalcode.minions.database.DatabaseConfig;
import com.eternalcode.minions.database.MinionDatabase;
import com.eternalcode.minions.database.MinionPersistenceService;
import com.eternalcode.minions.event.MinionEventCause;
import com.eternalcode.minions.event.MinionEventDispatcher;
import com.eternalcode.minions.gui.MinionPanel;
import com.eternalcode.minions.gui.MinionUpgradePanel;
import com.eternalcode.minions.item.MinionAppearanceItems;
import com.eternalcode.minions.item.MinionItemFactory;
import com.eternalcode.minions.item.MinionItemServiceImpl;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionActionEngine;
import com.eternalcode.minions.minion.MinionBehaviorRegistry;
import com.eternalcode.minions.minion.MinionBehaviorServiceImpl;
import com.eternalcode.minions.minion.MinionIdSequence;
import com.eternalcode.minions.minion.MinionLifecycleService;
import com.eternalcode.minions.minion.MinionManagement;
import com.eternalcode.minions.minion.MinionPistonProtectionListener;
import com.eternalcode.minions.minion.MinionPlacementListener;
import com.eternalcode.minions.minion.MinionQueries;
import com.eternalcode.minions.minion.MinionRegistry;
import com.eternalcode.minions.minion.MinionRotationService;
import com.eternalcode.minions.minion.MinionSnapshotMapper;
import com.eternalcode.minions.minion.access.MinionAccessGuard;
import com.eternalcode.minions.minion.access.MinionAccessServiceImpl;
import com.eternalcode.minions.minion.activity.MinionActivityBypass;
import com.eternalcode.minions.minion.activity.MinionActivityService;
import com.eternalcode.minions.minion.activity.rule.loadedchunk.LoadedChunkActivityRule;
import com.eternalcode.minions.minion.activity.rule.offline.OfflineActivityRule;
import com.eternalcode.minions.minion.activity.rule.proximity.ProximityActivityRule;
import com.eternalcode.minions.minion.impl.killer.KillerLootingListener;
import com.eternalcode.minions.minion.impl.collector.CollectorConfig;
import com.eternalcode.minions.minion.impl.crafter.CrafterConfig;
import com.eternalcode.minions.minion.impl.farmer.FarmerConfig;
import com.eternalcode.minions.minion.impl.fisherman.FishermanConfig;
import com.eternalcode.minions.minion.impl.killer.KillerConfig;
import com.eternalcode.minions.minion.impl.lumberjack.LumberjackConfig;
import com.eternalcode.minions.minion.impl.miner.MinerConfig;
import com.eternalcode.minions.minion.impl.seller.SellerConfig;
import com.eternalcode.minions.minion.limit.PlayerMinionLimitService;
import com.eternalcode.minions.minion.status.CoreMinionStatuses;
import com.eternalcode.minions.minion.status.MinionStatusServiceImpl;
import com.eternalcode.minions.minion.status.MinionStatusTracker;
import com.eternalcode.minions.minion.storage.ChestLinkService;
import com.eternalcode.minions.minion.storage.MinionItemTransferService;
import com.eternalcode.minions.minion.tool.MinionToolService;
import com.eternalcode.minions.minion.tool.ToolDurabilityService;
import com.eternalcode.minions.minion.tool.ToolInventoryLocator;
import com.eternalcode.minions.minion.tool.ToolValidationService;
import com.eternalcode.minions.minion.upgrade.MinionUpgradeService;
import com.eternalcode.minions.notice.NoticeResultHandler;
import com.eternalcode.minions.notice.NoticeService;
import com.eternalcode.minions.render.MinionEntityIndex;
import com.eternalcode.minions.render.MinionInteractionListener;
import com.eternalcode.minions.render.MinionRenderService;
import com.eternalcode.minions.render.MinionRenderer;
import com.eternalcode.minions.render.MinionViewerListener;
import com.eternalcode.minions.shop.MinionShopProvider;
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
import java.util.stream.Stream;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class EternalMinionsPlugin extends JavaPlugin {

    private final MinionPickupHandler minionPickupHandler = new MinionPickupHandler();
    private MinionRegistry minions;
    private MinionAccessServiceImpl minionAccess;
    private MinionShopServiceImpl shopService;
    private MinionRenderer renderer;
    private MinionDatabase database;
    private MinionInteractionListener interactions;
    private LiteCommands<CommandSender> commands;
    private BukkitTask tickTask;
    private boolean apiInitialized;

    @Override
    public void onEnable() {
        File dataFolder = this.getDataFolder();
        dataFolder.mkdirs();

        ConfigService configs = new ConfigService(dataFolder.toPath(), List.of(
                MinionsConfig.class,
                MessagesConfig.class,
                MinionPanelConfig.class,
                DatabaseConfig.class,
                MinerConfig.class,
                LumberjackConfig.class,
                FarmerConfig.class,
                FishermanConfig.class,
                KillerConfig.class,
                CollectorConfig.class,
                CrafterConfig.class,
                SellerConfig.class
        ));

        MinionsConfig minionsConfig = configs.get(MinionsConfig.class);
        MessagesConfig messages = configs.get(MessagesConfig.class);
        MinionPanelConfig panelConfig = configs.get(MinionPanelConfig.class);
        DatabaseConfig databaseConfig = configs.get(DatabaseConfig.class);

        MiniMessage miniMessage = MiniMessage.miniMessage();
        NoticeService notices = new NoticeService(messages, miniMessage);
        MinionBehaviorRegistry behaviors = new MinionBehaviorRegistry();
        MinionItemTransferService itemTransfers = new MinionItemTransferService(minionsConfig);
        MinionToolService tools = new MinionToolService(
                new ToolValidationService(),
                new ToolDurabilityService(),
                new ToolInventoryLocator(),
                itemTransfers
        );
        KillerLootingListener killerLooting = new KillerLootingListener();
        SellerConfig sellerConfig = configs.get(SellerConfig.class);

        BridgeManager bridgeManager = new BridgeManager();
        Optional<VaultEconomyHook> economy = VaultBridge.discover(bridgeManager, this);
        List<MinionShopProvider> shopHooks = ShopBridges.discover(
                bridgeManager,
                this,
                sellerConfig.sellPrices,
                economy
        );
        this.shopService = new MinionShopServiceImpl(this.getLogger(), shopHooks);
        this.getServer().getPluginManager().registerEvents(this.shopService, this);
        this.getServer().getPluginManager().registerEvents(killerLooting, this);

        behaviors.replace(MinionBehaviorRegistry.createEnabled(
                configs,
                tools,
                itemTransfers,
                killerLooting,
                this.shopService
        ));

        MinionAppearanceItems appearance = new MinionAppearanceItems(this.getServer());
        this.minions = new MinionRegistry();
        PlayerMinionLimitService playerLimits = new PlayerMinionLimitService(this.minions, minionsConfig.limits);
        this.minionAccess = new MinionAccessServiceImpl(this.getLogger());
        MinionAccessGuard access = new MinionAccessGuard(this.minions, this.minionAccess, messages, notices);

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
        MinionRenderService renders =
                new MinionRenderService(this.getServer(), this.minions, this.renderer, minionsConfig);

        this.database = MinionDatabase.open(this.getLogger(), dataFolder, databaseConfig);
        MinionPersistenceService persistence = this.database.persistence();
        MinionActivityService activityService = new MinionActivityService(
                this.getServer(),
                new MinionActivityBypass(minionsConfig.activity.bypass),
                List.of(
                        new LoadedChunkActivityRule(minionsConfig.activity.loadedChunk),
                        new OfflineActivityRule(minionsConfig.activity.offline),
                        new ProximityActivityRule(minionsConfig.activity.proximity)
                )
        );
        MinionActionEngine actions = new MinionActionEngine(
                this.getServer(),
                this.minions,
                minionsConfig,
                behaviors,
                persistence,
                statusTracker,
                this.renderer,
                activityService
        );
        MinionItemFactory minionItems = new MinionItemFactory(this, behaviors, appearance, miniMessage);
        MinionSnapshotMapper snapshots = new MinionSnapshotMapper(statusTracker);
        MinionEventDispatcher events = new MinionEventDispatcher(this.getServer());
        MinionLifecycleService lifecycle = new MinionLifecycleService(
                this.minions,
                actions,
                renders,
                persistence,
                minionItems,
                behaviors,
                access,
                itemTransfers,
                statusTracker,
                snapshots,
                events
        );
        MinionIdSequence minionIds = new MinionIdSequence();

        MinionQueries queryApi = new MinionQueries(this.minions, snapshots);
        MinionManagement managementApi = new MinionManagement(this.minions, lifecycle, behaviors, minionIds, snapshots);
        MinionBehaviorServiceImpl behaviorApi = new MinionBehaviorServiceImpl(behaviors);
        MinionItemServiceImpl itemApi = new MinionItemServiceImpl(minionItems, behaviors, this.minions);
        MinionStatusServiceImpl statusApi = new MinionStatusServiceImpl(this.minions, statusTracker, snapshots, events);

        BiConsumer<Player, Minion> pickup = this.minionPickupHandler.createPickupHandler(lifecycle, playerLimits, notices, messages);
        MinionUpgradeService upgrades = new MinionUpgradeService(
                behaviors,
                access,
                (minion, kind, actorId) -> lifecycle.updateUpgrade(
                        minion,
                        kind,
                        MinionEventCause.UPGRADE_PURCHASE,
                        actorId),
                messages,
                notices,
                economy,
                snapshots,
                events
        );
        MinionUpgradePanel upgradePanel = new MinionUpgradePanel(
                this,
                panelConfig,
                miniMessage,
                behaviors,
                upgrades,
                access
        );
        ChestLinkService chestLinks = new ChestLinkService(
                access,
                minionsConfig,
                lifecycle::updateChestLink,
                messages,
                notices
        );
        MinionRotationService rotations = new MinionRotationService(
                access,
                lifecycle::updateSettings,
                (player, notice) -> notices.create().viewer(player).notice(notice).send(),
                messages.minionRotated
        );
        MinionPanel panel = new MinionPanel(
                this,
                panelConfig,
                messages,
                notices,
                miniMessage,
                behaviors,
                lifecycle,
                rotations,
                access,
                itemTransfers,
                pickup,
                upgradePanel::open,
                chestLinks::toggle
        );
        this.interactions = new MinionInteractionListener(this, entityIndex, access, panel, rotations, pickup);
        PacketEvents.getAPI().getEventManager().registerListener(this.interactions);

        Stream.of(
                this.minionAccess,
                new MinionViewerListener(renders),
                new MinionPistonProtectionListener(this.minions),
                chestLinks,
                new MinionPlacementListener(
                        minionItems,
                        minionIds,
                        lifecycle,
                        behaviors,
                        messages,
                        notices,
                        playerLimits)
        ).forEach(listener -> this.getServer().getPluginManager().registerEvents(listener, this));

        Runnable reload = () -> {
            configs.reload();
            behaviors.replace(MinionBehaviorRegistry.createEnabled(
                    configs,
                    tools,
                    itemTransfers,
                    killerLooting,
                    this.shopService
            ));
        };
        this.commands = LiteBukkitFactory.builder("eternalminions", this, this.getServer())
                .argumentSuggestion(String.class, ArgumentKey.of("type"), SuggestionResult.of(behaviors.ids()))
                .commands(
                        new ReloadCommand(reload, messages),
                        new MinionGiveCommand(minionItems, behaviors, messages)
                )
                .result(Notice.class, new NoticeResultHandler(notices))
                .message(LiteBukkitMessages.PLAYER_NOT_FOUND, messages.playerNotFound)
                .message(LiteBukkitMessages.PLAYER_ONLY, messages.playerOnly)
                .invalidUsage(new InvalidUsageHandlerImpl(notices, messages))
                .missingPermission(new MissingPermissionHandlerImpl(notices, messages))
                .extension(new LiteAdventureExtension<>())
                .build();

        this.tickTask = this.getServer().getScheduler().runTaskTimer(
                this,
                () -> {
                    actions.run();
                    this.renderer.tick(this.getServer().getCurrentTick());
                },
                1L,
                1L
        );

        this.restore(lifecycle);
        EternalMinionsProvider.initialize(new EternalMinionsApiImpl(
                queryApi,
                managementApi,
                behaviorApi,
                itemApi,
                statusApi,
                this.minionAccess,
                this.shopService
        ));
        this.apiInitialized = true;
        this.getLogger().info("EternalMinions initialized with renderer " + minionsConfig.minionRenderer + ".");
    }

    private void restore(MinionLifecycleService lifecycle) {
        this.database.initialize()
                .thenCompose(ignored -> this.database.loadAll())
                .whenComplete((loaded, error) -> {
                    if (error != null) {
                        this.getLogger().log(
                                Level.SEVERE,
                                "Database initialization failed; minions will run in memory only",
                                error
                        );
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
    }

    @Override
    public void onDisable() {
        if (this.tickTask != null) {
            this.tickTask.cancel();
        }
        if (this.commands != null) {
            this.commands.unregister();
        }
        if (this.interactions != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(this.interactions);
        }
        if (this.minions != null && this.renderer != null) {
            for (Minion minion : this.minions.minions()) {
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
}
