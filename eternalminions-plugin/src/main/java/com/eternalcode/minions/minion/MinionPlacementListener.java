package com.eternalcode.minions.minion;

import com.eternalcode.minions.config.MessagesConfig;
import com.eternalcode.minions.item.MinionItemFactory;
import com.eternalcode.minions.minion.miner.MiningMode;
import com.eternalcode.minions.notice.NoticeService;
import java.util.Optional;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class MinionPlacementListener implements Listener {

    private final MinionItemFactory items;
    private final MinionIdSequence ids;
    private final MinionLifecycleService lifecycle;
    private final MinionTypeService types;
    private final MessagesConfig messages;
    private final NoticeService notices;

    public MinionPlacementListener(
        MinionItemFactory items,
        MinionIdSequence ids,
        MinionLifecycleService lifecycle,
        MinionTypeService types,
        MessagesConfig messages,
        NoticeService notices
    ) {
        this.items = items;
        this.ids = ids;
        this.lifecycle = lifecycle;
        this.types = types;
        this.messages = messages;
        this.notices = notices;
    }

    @EventHandler
    public void onPlace(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (!this.items.isMinion(item)) {
            return;
        }
        event.setCancelled(true);

        Block target = event.getClickedBlock().getRelative(event.getBlockFace());
        Player player = event.getPlayer();
        if (!target.isEmpty() || !target.getRelative(0, 1, 0).isEmpty()) {
            this.notices.create().viewer(player).notice(this.messages.minionPlacementBlocked).send();
            return;
        }

        Optional<MinionItemFactory.StoredMinionState> state = this.items.readState(item);
        String behaviorId = state.map(MinionItemFactory.StoredMinionState::behaviorId)
            .orElseGet(() -> this.types.defaultType().id());
        MinionType type = this.types.type(behaviorId).orElse(null);
        if (type == null) {
            this.notices.create().viewer(player).notice(this.messages.minionTypeUnknown).send();
            return;
        }

        MinionUpgrades upgrades = state.map(MinionItemFactory.StoredMinionState::upgrades)
            .orElseGet(MinionUpgrades::none);
        MiningMode miningMode = state.map(MinionItemFactory.StoredMinionState::miningMode).orElse(MiningMode.SQUARE);
        MinionDirection direction = MinionDirection.fromYaw(player.getLocation().getYaw());
        Minion minion = new Minion(
            this.ids.next(),
            player.getUniqueId(),
            behaviorId,
            new MinionPosition(target.getWorld().getKey().asString(), target.getX(), target.getY(), target.getZ()),
            true,
            state.map(stored -> new MinionProgress(stored.level(), stored.progress()))
                .orElseGet(MinionProgress::start),
            new MinionEquipment(state.map(MinionItemFactory.StoredMinionState::tool).orElse(null)),
            this.createStorage(state.orElse(null), type, upgrades),
            upgrades,
            null,
            new MinionSettings(direction, miningMode)
        );
        this.lifecycle.add(minion);

        item.subtract(1);
        this.notices.create().viewer(player).notice(this.messages.minionPlaced).send();
    }

    private MinionStorage createStorage(MinionItemFactory.StoredMinionState state, MinionType type, MinionUpgrades upgrades) {
        int capacity = type.storageCapacity(upgrades);
        if (state == null || state.storage().length == 0) {
            return new MinionStorage(capacity);
        }
        MinionStorage storage = MinionStorage.of(state.storage());
        return capacity > storage.capacity() ? storage.resized(capacity) : storage;
    }
}
