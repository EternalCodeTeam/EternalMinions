package com.eternalcode.minions.item;

import static org.assertj.core.api.Assertions.assertThat;

import com.eternalcode.minions.config.AbstractMinionConfig;
import com.eternalcode.minions.item.MinionItemFactory.StoredMinionState;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionBehavior;
import com.eternalcode.minions.minion.MinionBehaviorRegistry;
import com.eternalcode.minions.minion.MinionContext;
import com.eternalcode.minions.minion.MinionEquipment;
import com.eternalcode.minions.minion.MinionId;
import com.eternalcode.minions.minion.MinionPosition;
import com.eternalcode.minions.minion.MinionProgress;
import com.eternalcode.minions.minion.MinionResult;
import com.eternalcode.minions.minion.storage.MinionSettings;
import com.eternalcode.minions.minion.storage.MinionStorage;
import com.eternalcode.minions.minion.upgrade.DefaultUpgradeKinds;
import com.eternalcode.minions.minion.upgrade.MinionUpgrades;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.plugin.PluginMock;

class MinionItemFactoryTest {

    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private ServerMock server;
    private PluginMock plugin;
    private MinionItemFactory items;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.plugin = MockBukkit.createMockPlugin("EternalMinionsTest");

        MinionBehaviorRegistry behaviors = new MinionBehaviorRegistry();
        behaviors.replace(List.of(testBehavior()));

        this.items = new MinionItemFactory(
                this.plugin,
                behaviors,
                new MinionAppearanceItems(this.server),
                MiniMessage.miniMessage()
        );
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void shouldRoundTripFullMinionStateThroughItemPersistentData() {
        ItemStack tool = new ItemStack(Material.DIAMOND_PICKAXE);
        MinionStorage storage = new MinionStorage(9)
                .withItem(0, new ItemStack(Material.COBBLESTONE, 32))
                .withItem(5, new ItemStack(Material.DIAMOND, 3));
        Minion minion = new Minion(
                new MinionId(42),
                OWNER_ID,
                "TEST",
                new MinionPosition("world", 1, 64, 1),
                new MinionProgress(5, 1_234L),
                new MinionEquipment(tool),
                storage,
                MinionUpgrades.none().withTier(DefaultUpgradeKinds.SPEED, 2),
                null,
                MinionSettings.defaults()
        );

        ItemStack minionItem = this.items.create(minion);
        StoredMinionState state = this.items.readState(minionItem).orElseThrow();

        assertThat(state.behaviorId()).isEqualTo("TEST");
        assertThat(state.level()).isEqualTo(5);
        assertThat(state.progress()).isEqualTo(1_234L);
        assertThat(state.tool()).isEqualTo(tool);
        assertThat(state.storage()[0]).isEqualTo(new ItemStack(Material.COBBLESTONE, 32));
        assertThat(state.storage()[5]).isEqualTo(new ItemStack(Material.DIAMOND, 3));
        assertThat(state.upgrades().tier(DefaultUpgradeKinds.SPEED)).isEqualTo(2);
    }

    @Test
    void shouldRoundTripAMinionWithNoToolAndEmptyStorage() {
        Minion minion = new Minion(
                new MinionId(1),
                OWNER_ID,
                "TEST",
                new MinionPosition("world", 0, 64, 0),
                MinionProgress.start(),
                MinionEquipment.empty(),
                new MinionStorage(9),
                MinionUpgrades.none(),
                null,
                MinionSettings.defaults()
        );

        ItemStack minionItem = this.items.create(minion);
        StoredMinionState state = this.items.readState(minionItem).orElseThrow();

        assertThat(state.tool()).isNull();
        assertThat(state.storage()).containsOnlyNulls();
        assertThat(state.upgrades().entries()).isEmpty();
    }

    @Test
    void shouldIdentifyMinionItemsButRejectPlainItemsAndAir() {
        Minion minion = new Minion(
                new MinionId(1), OWNER_ID, "TEST",
                new MinionPosition("world", 0, 64, 0), MinionProgress.start(),
                MinionEquipment.empty(), new MinionStorage(9), MinionUpgrades.none(),
                null, MinionSettings.defaults()
        );
        ItemStack minionItem = this.items.create(minion);

        assertThat(this.items.isMinion(minionItem)).isTrue();
        assertThat(this.items.isMinion(new ItemStack(Material.DIAMOND_PICKAXE))).isFalse();
        assertThat(this.items.isMinion(new ItemStack(Material.AIR))).isFalse();
        assertThat(this.items.isMinion(null)).isFalse();
        assertThat(this.items.readState(new ItemStack(Material.DIAMOND_PICKAXE))).isEmpty();
    }

    @Test
    void shouldIgnoreMalformedOrUnknownUpgradeEntriesInsteadOfFailingToLoad() {
        Minion minion = new Minion(
                new MinionId(1), OWNER_ID, "TEST",
                new MinionPosition("world", 0, 64, 0), MinionProgress.start(),
                MinionEquipment.empty(), new MinionStorage(9), MinionUpgrades.none(),
                null, MinionSettings.defaults()
        );
        ItemStack minionItem = this.items.create(minion);
        ItemMeta meta = minionItem.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        NamespacedKey upgradesKey = new NamespacedKey(this.plugin, "minion_upgrades");
        // An invalid-format kind key and a corrupted (non-numeric) tier value - both must be
        // skipped by the defensive try/catch rather than blocking the whole item from loading.
        data.set(upgradesKey, PersistentDataType.STRING, "bad-kind:2,SPEED:not-a-number,RANGE:1");
        minionItem.setItemMeta(meta);

        StoredMinionState state = this.items.readState(minionItem).orElseThrow();

        assertThat(state.upgrades().tier(DefaultUpgradeKinds.RANGE)).isEqualTo(1);
        assertThat(state.upgrades().entries()).hasSize(1);
    }

    private static MinionBehavior testBehavior() {
        AbstractMinionConfig config = new AbstractMinionConfig() {
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
            public AbstractMinionConfig config() {
                return config;
            }

            @Override
            public MinionResult execute(MinionContext context) {
                throw new UnsupportedOperationException("not exercised by this test");
            }
        };
    }
}
