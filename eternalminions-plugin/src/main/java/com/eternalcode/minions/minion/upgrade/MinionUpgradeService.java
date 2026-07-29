package com.eternalcode.minions.minion.upgrade;

import com.eternalcode.minions.access.MinionAccessAction;
import com.eternalcode.minions.minion.access.MinionAccessGuard;
import com.eternalcode.minions.config.MessagesConfig;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionBehavior;
import com.eternalcode.minions.minion.MinionBehaviorRegistry;
import com.eternalcode.minions.notice.NoticeService;
import com.eternalcode.minions.bridge.vault.EconomyService;
import com.eternalcode.multification.notice.Notice;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.BiConsumer;
import org.bukkit.entity.Player;

public final class MinionUpgradeService {

    private final MinionBehaviorRegistry behaviors;
    private final MinionAccessGuard access;
    private final BiConsumer<Minion, UpgradeKind> update;
    private final MessagesConfig messages;
    private final NoticeService notices;
    private final UpgradePayment payment;

    public MinionUpgradeService(
        MinionBehaviorRegistry behaviors,
        MinionAccessGuard access,
        BiConsumer<Minion, UpgradeKind> update,
        MessagesConfig messages,
        NoticeService notices,
        Optional<? extends EconomyService> economy
    ) {
        this.behaviors = behaviors;
        this.access = access;
        this.update = update;
        this.messages = messages;
        this.notices = notices;
        this.payment = new UpgradePayment(economy);
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

        if (!this.payment.withdraw(player.getUniqueId(), nextTier.costAmount())) {
            this.send(player, this.messages.upgradeCannotAfford);
            return Optional.empty();
        }

        Minion updated = minion.withUpgrades(minion.upgrades().withTier(kind, currentTier + 1));
        if (kind.equals(CoreUpgradeKinds.CAPACITY)) {
            updated = updated.withStorage(updated.storage().resized(behavior.storageCapacity(updated)));
        }
        this.update.accept(updated, kind);
        this.send(player, this.messages.upgradePurchased);
        return Optional.of(updated);
    }

    public String formatCost(BigDecimal amount) {
        return this.payment.format(amount);
    }

    private void send(Player player, Notice notice) {
        this.notices.create().viewer(player).notice(notice).send();
    }
}
