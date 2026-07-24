package com.eternalcode.minions.minion;

import com.eternalcode.minions.config.MessagesConfig;
import com.eternalcode.minions.notice.NoticeService;
import com.eternalcode.multification.notice.Notice;
import java.util.Optional;
import java.util.function.BiConsumer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class MinionUpgradeService {

    private final MinionTypeService types;
    private final BiConsumer<Minion, MinionUpgradeKind> update;
    private final MessagesConfig messages;
    private final NoticeService notices;

    public MinionUpgradeService(
        MinionTypeService types,
        BiConsumer<Minion, MinionUpgradeKind> update,
        MessagesConfig messages,
        NoticeService notices
    ) {
        this.types = types;
        this.update = update;
        this.messages = messages;
        this.notices = notices;
    }

    public Optional<Minion> purchase(Player player, Minion minion, MinionUpgradeKind kind) {
        MinionType type = this.types.type(minion.behaviorId()).orElse(null);
        if (type == null) {
            this.send(player, this.messages.minionTypeUnknown);
            return Optional.empty();
        }

        int currentTier = minion.upgrades().tier(kind);
        if (currentTier >= type.maxUpgradeTier(kind)) {
            this.send(player, this.messages.upgradeMaxed);
            return Optional.empty();
        }

        MinionUpgradeTier nextTier = type.upgradeTier(kind, currentTier + 1);
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
        if (kind == MinionUpgradeKind.CAPACITY) {
            updated = updated.withStorage(updated.storage().resized(type.storageCapacity(updated.upgrades())));
        }
        this.update.accept(updated, kind);
        this.send(player, this.messages.upgradePurchased);
        return Optional.of(updated);
    }

    private void send(Player player, Notice notice) {
        this.notices.create().viewer(player).notice(notice).send();
    }
}
