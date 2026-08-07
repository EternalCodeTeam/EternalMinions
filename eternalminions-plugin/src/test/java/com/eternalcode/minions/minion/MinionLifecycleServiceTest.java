package com.eternalcode.minions.minion;

import static org.assertj.core.api.Assertions.assertThat;

import com.eternalcode.minions.config.MinionsConfig;
import com.eternalcode.minions.database.DatabaseManager;
import com.eternalcode.minions.database.DatabaseSettings;
import com.eternalcode.minions.database.MinionPersistenceService;
import com.eternalcode.minions.database.repository.MinionEquipmentRepository;
import com.eternalcode.minions.database.repository.MinionRepository;
import com.eternalcode.minions.database.repository.MinionStateRepository;
import com.eternalcode.minions.event.MinionEventCause;
import com.eternalcode.minions.event.MinionEventDispatcher;
import com.eternalcode.minions.event.MinionPreCreateEvent;
import com.eternalcode.minions.event.MinionPreRemoveEvent;
import com.eternalcode.minions.item.MinionAppearanceItems;
import com.eternalcode.minions.item.MinionItemFactory;
import com.eternalcode.minions.minion.access.MinionAccessGuard;
import com.eternalcode.minions.minion.access.MinionAccessServiceImpl;
import com.eternalcode.minions.config.MessagesConfig;
import com.eternalcode.minions.minion.storage.MinionChestLinkRepository;
import com.eternalcode.minions.minion.storage.MinionItemTransferService;
import com.eternalcode.minions.minion.storage.MinionSettings;
import com.eternalcode.minions.minion.storage.MinionSettingsRepository;
import com.eternalcode.minions.minion.storage.MinionStorage;
import com.eternalcode.minions.minion.status.CoreMinionStatuses;
import com.eternalcode.minions.minion.status.MinionStatusTracker;
import com.eternalcode.minions.minion.upgrade.MinionUpgradeRepository;
import com.eternalcode.minions.minion.upgrade.MinionUpgrades;
import com.eternalcode.minions.notice.NoticeService;
import com.eternalcode.minions.render.MinionRenderService;
import com.eternalcode.minions.render.MinionRenderer;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

class MinionLifecycleServiceTest {

    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private ServerMock server;
    private PluginMock plugin;
    private PlayerMock owner;
    private MinionRegistry minions;
    private MinionActionEngine actions;
    private MinionEventDispatcher events;
    private MinionLifecycleService lifecycle;
    private MinionItemFactory items;
    private MinionItemTransferService transfers;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.plugin = MockBukkit.createMockPlugin("EternalMinionsTest");
        org.mockbukkit.mockbukkit.world.WorldMock world = this.server.addSimpleWorld("world");
        this.owner = new PlayerMock(this.server, "Owner", OWNER_ID);
        this.owner.setLocation(new org.bukkit.Location(world, 0, 64, 0));
        this.server.addPlayer(this.owner);

        this.minions = new MinionRegistry();
        this.events = new MinionEventDispatcher(this.server);

        MinionsConfig config = new MinionsConfig();
        MinionBehaviorRegistry behaviors = new MinionBehaviorRegistry();
        behaviors.replace(List.of(testBehavior()));

        this.items = new MinionItemFactory(
                this.plugin, behaviors, new MinionAppearanceItems(this.server), MiniMessage.miniMessage()
        );
        this.transfers = new MinionItemTransferService(config);

        MinionRenderer noopRenderer = new MinionRenderer() {
            @Override
            public void show(org.bukkit.entity.Player player, Minion minion) {
            }

            @Override
            public void hide(org.bukkit.entity.Player player, MinionId minionId) {
            }

            @Override
            public void remove(MinionId minionId) {
            }

            @Override
            public void animate(MinionId minionId, float targetYaw) {
            }
        };
        MinionRenderService renders = new MinionRenderService(this.server, this.minions, noopRenderer, config);

        MinionStatusTracker statuses = new MinionStatusTracker(CoreMinionStatuses.IDLE);
        MinionSnapshotMapper snapshots = new MinionSnapshotMapper(statuses);
        MinionAccessGuard access = new MinionAccessGuard(
                this.minions,
                new MinionAccessServiceImpl(Logger.getAnonymousLogger()),
                new MessagesConfig(),
                mock(NoticeService.class, RETURNS_DEEP_STUBS)
        );

        com.eternalcode.minions.minion.activity.MinionActivityService activity =
                new com.eternalcode.minions.minion.activity.MinionActivityService(
                        this.server,
                        new com.eternalcode.minions.minion.activity.MinionActivityBypass(
                                new com.eternalcode.minions.minion.activity.config.ActivityBypassConfig()
                        ),
                        List.of()
                );
        this.actions = new MinionActionEngine(
                this.server, this.minions, config, behaviors,
                unreadyPersistenceService(), statuses, noopRenderer, activity
        );

        this.lifecycle = new MinionLifecycleService(
                this.minions, this.actions, renders, unreadyPersistenceService(),
                this.items, behaviors, access, this.transfers, statuses, snapshots, this.events
        );
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void shouldNotDuplicateStorageContentsWhenPickingUpAMinionWithItemsInStorage() {
        MinionStorage storage = new MinionStorage(9)
                .withItem(0, new ItemStack(Material.DIAMOND, 5))
                .withItem(3, new ItemStack(Material.IRON_INGOT, 10));
        Minion minion = new Minion(
                new MinionId(1), OWNER_ID, "TEST",
                new MinionPosition(this.owner.getWorld().getKey().asString(), 0, 64, 0),
                MinionProgress.start(), MinionEquipment.empty(), storage, MinionUpgrades.none(),
                null, MinionSettings.defaults()
        );
        this.minions.register(minion);
        this.actions.add(minion);

        boolean pickedUp = this.lifecycle.pickup(this.owner, minion);

        assertThat(pickedUp).isTrue();
        assertThat(this.owner.getInventory().all(Material.DIAMOND).values())
                .as("storage contents must be handed to the player exactly once")
                .extracting(ItemStack::getAmount)
                .containsExactly(5);
        assertThat(this.owner.getInventory().all(Material.IRON_INGOT).values())
                .extracting(ItemStack::getAmount)
                .containsExactly(10);

        ItemStack minionItem = findMinionItem(this.owner.getInventory().getContents());
        MinionItemFactory.StoredMinionState state = this.items.readState(minionItem).orElseThrow();
        assertThat(state.storage())
                .as("the picked-up minion item must carry empty storage, or placing it again would duplicate items")
                .containsOnlyNulls();
    }

    @Test
    void shouldNotRemoveMinionWhenPreRemoveEventIsCancelled() {
        Minion minion = registeredMinion(1);
        this.server.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            void onPreRemove(MinionPreRemoveEvent event) {
                event.setCancelled(true);
            }
        }, this.plugin);

        boolean removed = this.lifecycle.remove(minion.id(), MinionEventCause.API, OWNER_ID);

        assertThat(removed).isFalse();
        assertThat(this.minions.findMinion(minion.id())).isPresent();
    }

    @Test
    void shouldNotAddMinionWhenPreCreateEventIsCancelled() {
        Minion minion = new Minion(
                new MinionId(5), OWNER_ID, "TEST",
                new MinionPosition("world", 5, 64, 5), MinionProgress.start(),
                MinionEquipment.empty(), new MinionStorage(9), MinionUpgrades.none(),
                null, MinionSettings.defaults()
        );
        this.server.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            void onPreCreate(MinionPreCreateEvent event) {
                event.setCancelled(true);
            }
        }, this.plugin);

        boolean added = this.lifecycle.add(minion, MinionEventCause.API, OWNER_ID);

        assertThat(added).isFalse();
        assertThat(this.minions.findMinion(minion.id())).isEmpty();
    }

    private ItemStack findMinionItem(ItemStack[] contents) {
        for (ItemStack item : contents) {
            if (item != null && this.items.isMinion(item)) {
                return item;
            }
        }
        throw new AssertionError("No minion item found in the player's inventory");
    }

    private Minion registeredMinion(long id) {
        Minion minion = new Minion(
                new MinionId(id), OWNER_ID, "TEST",
                new MinionPosition("world", (int) id, 64, 0), MinionProgress.start(),
                MinionEquipment.empty(), new MinionStorage(9), MinionUpgrades.none(),
                null, MinionSettings.defaults()
        );
        this.minions.register(minion);
        this.actions.add(minion);
        return minion;
    }

    private static MinionBehavior testBehavior() {
        com.eternalcode.minions.config.AbstractMinionConfig config =
                new com.eternalcode.minions.config.AbstractMinionConfig() {
                    @Override
                    public Path resolve(Path dataDirectory) {
                        return dataDirectory;
                    }
                };

        return new MinionBehavior() {
            @Override
            public String id() {
                return "TEST";
            }

            @Override
            public com.eternalcode.minions.config.AbstractMinionConfig config() {
                return config;
            }

            @Override
            public MinionResult execute(MinionContext context) {
                throw new UnsupportedOperationException("not exercised by this test");
            }
        };
    }

    private static MinionPersistenceService unreadyPersistenceService() {
        DatabaseManager databaseManager = new DatabaseManager(
                Logger.getAnonymousLogger(),
                new File("."),
                noopDatabaseSettings()
        );
        com.eternalcode.commons.scheduler.Scheduler scheduler =
                mock(com.eternalcode.commons.scheduler.Scheduler.class);
        // MinionRepository.ready() defaults to false and is only flipped by markReady() after the
        // initial loadAll() at startup completes, so every persistence call below is a safe no-op -
        // exactly what these lifecycle tests need without standing up a real database.
        MinionRepository minionRepository = new MinionRepository(databaseManager, scheduler);
        return new MinionPersistenceService(
                Logger.getAnonymousLogger(),
                minionRepository,
                new MinionStateRepository(databaseManager, scheduler),
                new MinionSettingsRepository(databaseManager, scheduler),
                new MinionEquipmentRepository(databaseManager, scheduler),
                new com.eternalcode.minions.minion.storage.MinionStorageRepository(databaseManager, scheduler),
                new MinionUpgradeRepository(databaseManager, scheduler),
                new MinionChestLinkRepository(databaseManager, scheduler)
        );
    }

    private static DatabaseSettings noopDatabaseSettings() {
        return new DatabaseSettings() {
            @Override
            public com.eternalcode.minions.database.DatabaseDriverType databaseType() {
                return com.eternalcode.minions.database.DatabaseDriverType.H2;
            }

            @Override
            public String hostname() {
                return "localhost";
            }

            @Override
            public int port() {
                return 0;
            }

            @Override
            public String database() {
                return "test";
            }

            @Override
            public String username() {
                return "test";
            }

            @Override
            public String password() {
                return "";
            }

            @Override
            public boolean ssl() {
                return false;
            }

            @Override
            public int poolSize() {
                return 1;
            }

            @Override
            public int timeout() {
                return 1000;
            }
        };
    }
}
