package com.eternalcode.minions.minion.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import com.eternalcode.minions.config.MessagesConfig;
import com.eternalcode.minions.config.MinionsConfig;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionEquipment;
import com.eternalcode.minions.minion.MinionId;
import com.eternalcode.minions.minion.MinionPosition;
import com.eternalcode.minions.minion.MinionProgress;
import com.eternalcode.minions.minion.MinionRegistry;
import com.eternalcode.minions.minion.access.MinionAccessGuard;
import com.eternalcode.minions.minion.access.MinionAccessServiceImpl;
import com.eternalcode.minions.minion.upgrade.MinionUpgrades;
import com.eternalcode.minions.notice.NoticeService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.player.PlayerInteractEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

class ChestLinkServiceTest {

    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private ServerMock server;
    private WorldMock world;
    private PlayerMock owner;
    private MinionRegistry minions;
    private MinionsConfig config;
    private List<Minion> updates;
    private ChestLinkService chestLink;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
        this.world = this.server.addSimpleWorld("world");
        this.owner = new PlayerMock(this.server, "Owner", OWNER_ID);
        this.owner.setLocation(new Location(this.world, 0, 64, 0));
        this.server.addPlayer(this.owner);

        this.minions = new MinionRegistry();
        this.config = new MinionsConfig();
        this.config.chestLinkDistanceBlocks = 10;
        this.updates = new ArrayList<>();

        MinionAccessGuard access = new MinionAccessGuard(
                this.minions,
                new MinionAccessServiceImpl(Logger.getAnonymousLogger()),
                new MessagesConfig(),
                mock(NoticeService.class, RETURNS_DEEP_STUBS)
        );

        this.chestLink = new ChestLinkService(
                access,
                this.config,
                this.updates::add,
                new MessagesConfig(),
                mock(NoticeService.class, RETURNS_DEEP_STUBS)
        );
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void shouldLinkToAChestClickedWithinRangeAfterToggle() {
        Minion minion = minionAt(0, 64, 0);
        this.minions.register(minion);
        this.chestLink.toggle(this.owner, minion);
        Block chest = blockAt(Material.CHEST, 3, 64, 3);

        this.chestLink.onChestClick(rightClick(chest));

        assertThat(this.updates).hasSize(1);
        MinionPosition linked = this.updates.getFirst().chestPosition();
        assertThat(linked).isNotNull();
        assertThat(linked.blockX()).isEqualTo(3);
        assertThat(linked.blockY()).isEqualTo(64);
        assertThat(linked.blockZ()).isEqualTo(3);
    }

    @Test
    void shouldNotLinkWhenClickedChestIsBeyondTheConfiguredDistance() {
        Minion minion = minionAt(0, 64, 0);
        this.minions.register(minion);
        this.chestLink.toggle(this.owner, minion);
        Block farChest = blockAt(Material.CHEST, 100, 64, 100);

        this.chestLink.onChestClick(rightClick(farChest));

        assertThat(this.updates).isEmpty();
    }

    @Test
    void shouldNotLinkWhenClickedBlockIsNotAContainer() {
        Minion minion = minionAt(0, 64, 0);
        this.minions.register(minion);
        this.chestLink.toggle(this.owner, minion);
        Block plainBlock = blockAt(Material.STONE, 2, 64, 2);

        this.chestLink.onChestClick(rightClick(plainBlock));

        assertThat(this.updates).isEmpty();
    }

    @Test
    void shouldIgnoreChestClicksFromPlayersWithoutAPendingLinkRequest() {
        Block chest = blockAt(Material.CHEST, 1, 64, 1);

        this.chestLink.onChestClick(rightClick(chest));

        assertThat(this.updates).isEmpty();
    }

    @Test
    void shouldUnlinkAnAlreadyLinkedMinionOnToggle() {
        Minion linkedMinion = minionAt(0, 64, 0).withChestPosition(new MinionPosition("world", 3, 64, 3));
        this.minions.register(linkedMinion);

        this.chestLink.toggle(this.owner, linkedMinion);

        assertThat(this.updates).hasSize(1);
        assertThat(this.updates.getFirst().chestPosition()).isNull();
    }

    @Test
    void shouldNotConsumePendingLinkAfterAnUnrelatedRightClick() {
        Minion minion = minionAt(0, 64, 0);
        this.minions.register(minion);
        this.chestLink.toggle(this.owner, minion);
        Block plainBlock = blockAt(Material.STONE, 2, 64, 2);
        this.chestLink.onChestClick(rightClick(plainBlock));

        Block chest = blockAt(Material.CHEST, 3, 64, 3);
        this.chestLink.onChestClick(rightClick(chest));

        assertThat(this.updates)
                .as("a miss on a non-container block must not cancel the pending link request")
                .hasSize(1);
    }

    private Minion minionAt(int x, int y, int z) {
        return new Minion(
                new MinionId(1), OWNER_ID, "TEST",
                new MinionPosition("world", x, y, z), MinionProgress.start(),
                MinionEquipment.empty(), new MinionStorage(9), MinionUpgrades.none(),
                null, MinionSettings.defaults()
        );
    }

    private Block blockAt(Material material, int x, int y, int z) {
        Block block = this.world.getBlockAt(x, y, z);
        block.setType(material);
        return block;
    }

    private PlayerInteractEvent rightClick(Block block) {
        return new PlayerInteractEvent(
                this.owner,
                Action.RIGHT_CLICK_BLOCK,
                new ItemStack(Material.AIR),
                block,
                org.bukkit.block.BlockFace.UP,
                EquipmentSlot.HAND
        );
    }
}
