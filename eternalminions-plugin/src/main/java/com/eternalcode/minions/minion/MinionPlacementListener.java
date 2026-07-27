package com.eternalcode.minions.minion;

import com.eternalcode.minions.config.MessagesConfig;
import com.eternalcode.minions.item.MinionItemFactory;
import com.eternalcode.minions.minion.limit.MinionLimitStatus;
import com.eternalcode.minions.minion.limit.PlayerMinionLimitService;
import com.eternalcode.minions.minion.storage.MinionSettings;
import com.eternalcode.minions.minion.storage.MinionStorage;
import com.eternalcode.minions.minion.upgrade.MinionUpgrades;
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
    private final MinionBehaviorRegistry behaviors;
    private final MessagesConfig messages;
    private final NoticeService notices;
    private final PlayerMinionLimitService playerLimits;

    public MinionPlacementListener(
        MinionItemFactory items,
        MinionIdSequence ids,
        MinionLifecycleService lifecycle,
        MinionBehaviorRegistry behaviors,
        MessagesConfig messages,
        NoticeService notices,
        PlayerMinionLimitService playerLimits
    ) {
        this.items = items;
        this.ids = ids;
        this.lifecycle = lifecycle;
        this.behaviors = behaviors;
        this.messages = messages;
        this.notices = notices;
        this.playerLimits = playerLimits;
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

        MinionLimitStatus playerLimit = this.playerLimits.statusFor(player);
        if (playerLimit.reached()) {
            this.notices.create().viewer(player).notice(this.messages.minionLimitReached)
                .placeholder("{MINION_LIMIT_CURRENT}", Integer.toString(playerLimit.current()))
                .placeholder("{MINION_LIMIT_MAX}", playerLimit.maxDisplay())
                .send();
            return;
        }

        Optional<MinionItemFactory.StoredMinionState> state = this.items.readState(item);
        String behaviorId = state.map(MinionItemFactory.StoredMinionState::behaviorId)
            .orElseGet(() -> this.behaviors.defaultBehavior().id());
        MinionBehavior behavior = this.behaviors.find(behaviorId).orElse(null);
        if (behavior == null) {
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
            this.createStorage(state.orElse(null), behavior, upgrades),
            upgrades,
            null,
            new MinionSettings(direction, miningMode)
        );
        this.lifecycle.add(minion);

        item.subtract(1);
        MinionLimitStatus updatedLimit = this.playerLimits.statusFor(player);
        this.notices.create().viewer(player).notice(this.messages.minionPlaced)
            .placeholder("{MINION_LIMIT_CURRENT}", Integer.toString(updatedLimit.current()))
            .placeholder("{MINION_LIMIT_MAX}", updatedLimit.maxDisplay())
            .send();
    }

    private MinionStorage createStorage(
        MinionItemFactory.StoredMinionState state,
        MinionBehavior behavior,
        MinionUpgrades upgrades
    ) {
        int capacity = behavior.config().storageCapacity(upgrades);
        if (state == null || state.storage().length == 0) {
            return new MinionStorage(capacity);
        }
        MinionStorage storage = MinionStorage.of(state.storage());
        return capacity > storage.capacity() ? storage.resized(capacity) : storage;
    }
}
