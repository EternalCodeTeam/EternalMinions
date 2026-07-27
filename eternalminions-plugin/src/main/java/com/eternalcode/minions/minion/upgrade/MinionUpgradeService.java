package com.eternalcode.minions.minion.upgrade;

import com.eternalcode.minions.access.MinionAccessAction;
import com.eternalcode.minions.minion.access.MinionAccessGuard;
import com.eternalcode.minions.config.MessagesConfig;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionBehavior;
import com.eternalcode.minions.minion.MinionBehaviorRegistry;
import com.eternalcode.minions.notice.NoticeService;
import com.eternalcode.multification.notice.Notice;
import java.util.Optional;
import java.util.function.BiConsumer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class MinionUpgradeService {

    private final MinionBehaviorRegistry behaviors;
    private final MinionAccessGuard access;
    private final BiConsumer<Minion, UpgradeKind> update;
    private final MessagesConfig messages;
    private final NoticeService notices;

    public MinionUpgradeService(
        MinionBehaviorRegistry behaviors,
        MinionAccessGuard access,
        BiConsumer<Minion, UpgradeKind> update,
        MessagesConfig messages,
        NoticeService notices
    ) {
        this.behaviors = behaviors;
        this.access = access;
        this.update = update;
        this.messages = messages;
        this.notices = notices;
    }

    public Optional<Minion> purchase(Player player, Minion minion, UpgradeKind kind) {
        Minion current = this.access.findAccessible(
                player,
                minion.id(),
                MinionAccessAction.MANAGE
        ).orElse(null);

        if (current == null) {
            return Optional.empty();
        }

        minion = current;
        MinionBehavior behavior = this.behaviors.find(minion.behaviorId()).orElse(null);
        if (behavior == null) {
            this.send(player, this.messages.minionTypeUnknown);
            return Optional.empty();
        }

        int currentTier = minion.upgrades().tier(kind);
        if (currentTier >= behavior.config().maxUpgradeTier(kind)) {
            this.send(player, this.messages.upgradeMaxed);
            return Optional.empty();
        }

        MinionUpgradeTier nextTier = behavior.config().upgradeTier(kind, currentTier + 1);
        if (minion.progress().level() < nextTier.requiredLevel()) {
            this.send(player, this.messages.upgradeRequiresLevel);
            return Optional.empty();
        }

        ItemStack cost = new ItemStack(nextTier.costMaterial(), nextTier.costAmount());
        if (!player.getInventory().containsAtLeast(cost, nextTier.costAmount())) {
            this.send(player, this.messages.upgradeCannotAfford);
            return Optional.empty();
        }
        player.getInventory().removeItem(cost);

        Minion updated = minion.withUpgrades(minion.upgrades().withTier(kind, currentTier + 1));
        if (kind.equals(CoreUpgradeKinds.CAPACITY)) {
            updated = updated.withStorage(updated.storage().resized(behavior.storageCapacity(updated)));
        }
        this.update.accept(updated, kind);
        this.send(player, this.messages.upgradePurchased);
        return Optional.of(updated);
    }

    private void send(Player player, Notice notice) {
        this.notices.create().viewer(player).notice(notice).send();
    }
}
