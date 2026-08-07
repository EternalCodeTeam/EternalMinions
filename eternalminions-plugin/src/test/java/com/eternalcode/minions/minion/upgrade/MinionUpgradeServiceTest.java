package com.eternalcode.minions.minion.upgrade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import com.eternalcode.minions.bridge.vault.EconomyService;
import com.eternalcode.minions.config.AbstractMinionConfig;
import com.eternalcode.minions.config.MessagesConfig;
import com.eternalcode.minions.event.MinionEventDispatcher;
import com.eternalcode.minions.event.MinionUpgradePurchaseEvent;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionBehavior;
import com.eternalcode.minions.minion.MinionBehaviorRegistry;
import com.eternalcode.minions.minion.MinionContext;
import com.eternalcode.minions.minion.MinionEquipment;
import com.eternalcode.minions.minion.MinionId;
import com.eternalcode.minions.minion.MinionPosition;
import com.eternalcode.minions.minion.MinionProgress;
import com.eternalcode.minions.minion.MinionRegistry;
import com.eternalcode.minions.minion.MinionResult;
import com.eternalcode.minions.minion.MinionSnapshotMapper;
import com.eternalcode.minions.minion.access.MinionAccessGuard;
import com.eternalcode.minions.minion.access.MinionAccessServiceImpl;
import com.eternalcode.minions.minion.status.CoreMinionStatuses;
import com.eternalcode.minions.minion.status.MinionStatusTracker;
import com.eternalcode.minions.minion.storage.MinionSettings;
import com.eternalcode.minions.minion.storage.MinionStorage;
import com.eternalcode.minions.notice.NoticeService;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;

class MinionUpgradeServiceTest {

    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private ServerMock server;
    private PluginMock plugin;
    private PlayerMock owner;
    private MinionRegistry minions;
    private MinionBehaviorRegistry behaviors;
    private FakeEconomyService economy;
    private List<UpgradeApplication> applications;
    private MinionUpgradeService upgradeService;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.plugin = MockBukkit.createMockPlugin("EternalMinionsTest");
        this.owner = new PlayerMock(this.server, "Owner", OWNER_ID);
        this.server.addPlayer(this.owner);

        this.minions = new MinionRegistry();
        this.behaviors = new MinionBehaviorRegistry();
        this.behaviors.replace(List.of(testBehavior()));
        this.economy = new FakeEconomyService();
        this.applications = new ArrayList<>();

        MinionAccessGuard access = new MinionAccessGuard(
                this.minions,
                new MinionAccessServiceImpl(Logger.getAnonymousLogger()),
                new MessagesConfig(),
                mock(NoticeService.class, RETURNS_DEEP_STUBS)
        );
        MinionSnapshotMapper snapshots =
                new MinionSnapshotMapper(new MinionStatusTracker(CoreMinionStatuses.IDLE));

        this.upgradeService = new MinionUpgradeService(
                this.behaviors,
                access,
                (minion, kind, actorId) -> this.applications.add(new UpgradeApplication(minion, kind, actorId)),
                new MessagesConfig(),
                mock(NoticeService.class, RETURNS_DEEP_STUBS),
                Optional.of(this.economy),
                snapshots,
                new MinionEventDispatcher(this.server)
        );
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void shouldRejectPurchaseFromNonOwner() {
        Minion minion = registerMinion(MinionProgress.start());
        PlayerMock stranger = this.server.addPlayer();

        Optional<Minion> result = this.upgradeService.purchase(stranger, minion, DefaultUpgradeKinds.SPEED);

        assertThat(result).isEmpty();
        assertThat(this.economy.balanceOf(stranger.getUniqueId())).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(this.applications).isEmpty();
    }

    @Test
    void shouldRejectPurchaseWhenMinionBehaviorIsUnknown() {
        Minion ghost = new Minion(
                new MinionId(99),
                OWNER_ID,
                "GHOST_BEHAVIOR",
                new MinionPosition("world", 0, 64, 0),
                MinionProgress.start(),
                MinionEquipment.empty(),
                new MinionStorage(9),
                MinionUpgrades.none(),
                null,
                MinionSettings.defaults()
        );
        this.minions.register(ghost);

        Optional<Minion> result = this.upgradeService.purchase(this.owner, ghost, DefaultUpgradeKinds.SPEED);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldRejectPurchaseWhenAlreadyAtMaximumTier() {
        Minion minion = registerMinion(new Minion(
                new MinionId(1), OWNER_ID, "TEST",
                new MinionPosition("world", 0, 64, 0),
                new MinionProgress(10, 0L), MinionEquipment.empty(),
                new MinionStorage(9),
                MinionUpgrades.none().withTier(DefaultUpgradeKinds.SPEED, 2),
                null, MinionSettings.defaults()
        ));

        Optional<Minion> result = this.upgradeService.purchase(this.owner, minion, DefaultUpgradeKinds.SPEED);

        assertThat(result).isEmpty();
        assertThat(this.applications).isEmpty();
    }

    @Test
    void shouldRejectPurchaseWhenMinionLevelIsBelowTheTierRequirement() {
        Minion minion = registerMinion(new MinionProgress(1, 0L));

        Optional<Minion> result = this.upgradeService.purchase(this.owner, minion, DefaultUpgradeKinds.SPEED);

        assertThat(result)
                .as("first SPEED tier requires level 2, minion is level 1")
                .isEmpty();
        assertThat(this.economy.balanceOf(OWNER_ID)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldRejectPurchaseWhenAPluginListenerCancelsTheEvent() {
        Minion minion = registerMinion(new MinionProgress(5, 0L));
        this.economy.deposit(OWNER_ID, new BigDecimal("100.00"));
        this.server.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            void onPurchase(MinionUpgradePurchaseEvent event) {
                event.setCancelled(true);
            }
        }, this.plugin);

        Optional<Minion> result = this.upgradeService.purchase(this.owner, minion, DefaultUpgradeKinds.SPEED);

        assertThat(result).isEmpty();
        assertThat(this.economy.balanceOf(OWNER_ID))
                .as("a cancelled purchase must never withdraw payment")
                .isEqualByComparingTo("100.00");
        assertThat(this.applications).isEmpty();
    }

    @Test
    void shouldRejectPurchaseWhenPlayerCannotAffordTheUpgrade() {
        Minion minion = registerMinion(new MinionProgress(5, 0L));
        this.economy.deposit(OWNER_ID, new BigDecimal("1.00"));

        Optional<Minion> result = this.upgradeService.purchase(this.owner, minion, DefaultUpgradeKinds.SPEED);

        assertThat(result).isEmpty();
        assertThat(this.economy.balanceOf(OWNER_ID))
                .as("a failed withdrawal must leave the balance untouched")
                .isEqualByComparingTo("1.00");
        assertThat(this.applications).isEmpty();
    }

    @Test
    void shouldApplyTierAndWithdrawPaymentOnSuccessfulPurchase() {
        Minion minion = registerMinion(new MinionProgress(5, 0L));
        this.economy.deposit(OWNER_ID, new BigDecimal("100.00"));

        Optional<Minion> result = this.upgradeService.purchase(this.owner, minion, DefaultUpgradeKinds.SPEED);

        assertThat(result).isPresent();
        assertThat(result.get().upgrades().tier(DefaultUpgradeKinds.SPEED)).isEqualTo(1);
        assertThat(this.economy.balanceOf(OWNER_ID))
                .isEqualByComparingTo(new BigDecimal("100.00").subtract(new BigDecimal("8.00")));
        assertThat(this.applications).hasSize(1);
        assertThat(this.applications.getFirst().kind()).isEqualTo(DefaultUpgradeKinds.SPEED);
        assertThat(this.applications.getFirst().actorId()).isEqualTo(OWNER_ID);
    }

    @Test
    void shouldGrowStorageWhenPurchasingACapacityUpgrade() {
        Minion minion = registerMinion(new MinionProgress(5, 0L));
        this.economy.deposit(OWNER_ID, new BigDecimal("100.00"));
        int originalCapacity = minion.storage().capacity();

        Optional<Minion> result = this.upgradeService.purchase(this.owner, minion, DefaultUpgradeKinds.CAPACITY);

        assertThat(result).isPresent();
        assertThat(result.get().storage().capacity())
                .isGreaterThan(originalCapacity)
                .isEqualTo(18);
    }

    private Minion registerMinion(MinionProgress progress) {
        return registerMinion(new Minion(
                new MinionId(1), OWNER_ID, "TEST",
                new MinionPosition("world", 0, 64, 0),
                progress, MinionEquipment.empty(),
                new MinionStorage(9), MinionUpgrades.none(),
                null, MinionSettings.defaults()
        ));
    }

    private Minion registerMinion(Minion minion) {
        this.minions.register(minion);
        return minion;
    }

    private static MinionBehavior testBehavior() {
        AbstractMinionConfig config = new AbstractMinionConfig() {
            @Override
            public Path resolve(Path dataDirectory) {
                return dataDirectory;
            }
        };
        config.upgrades = defaultUpgradeTiers();

        return new MinionBehavior() {
            @Override
            public String id() {
                return "TEST";
            }

            @Override
            public AbstractMinionConfig config() {
                return config;
            }

            @Override
            public MinionResult execute(MinionContext context) {
                throw new UnsupportedOperationException("not exercised by this test");
            }
        };
    }

    private static Map<UpgradeKind, List<com.eternalcode.minions.config.MinionUpgradeTierConfig>> defaultUpgradeTiers() {
        Map<UpgradeKind, List<com.eternalcode.minions.config.MinionUpgradeTierConfig>> upgrades = new HashMap<>();
        upgrades.put(DefaultUpgradeKinds.SPEED, List.of(
                new com.eternalcode.minions.config.MinionUpgradeTierConfig(2, 30, new BigDecimal("8.00")),
                new com.eternalcode.minions.config.MinionUpgradeTierConfig(3, 20, new BigDecimal("16.00"))
        ));
        upgrades.put(DefaultUpgradeKinds.CAPACITY, List.of(
                new com.eternalcode.minions.config.MinionUpgradeTierConfig(3, 18, new BigDecimal("8.00")),
                new com.eternalcode.minions.config.MinionUpgradeTierConfig(4, 27, new BigDecimal("16.00"))
        ));
        return upgrades;
    }

    private record UpgradeApplication(Minion minion, UpgradeKind kind, UUID actorId) {
    }

    private static final class FakeEconomyService implements EconomyService {

        private final Map<UUID, BigDecimal> balances = new HashMap<>();

        void deposit(UUID playerId, BigDecimal amount) {
            this.balances.merge(playerId, amount, BigDecimal::add);
        }

        BigDecimal balanceOf(UUID playerId) {
            return this.balances.getOrDefault(playerId, BigDecimal.ZERO);
        }

        @Override
        public boolean available() {
            return true;
        }

        @Override
        public boolean withdraw(UUID playerId, BigDecimal amount) {
            BigDecimal balance = this.balanceOf(playerId);
            if (balance.compareTo(amount) < 0) {
                return false;
            }
            this.balances.put(playerId, balance.subtract(amount));
            return true;
        }

        @Override
        public String format(BigDecimal amount) {
            return amount.toPlainString();
        }
    }
}
