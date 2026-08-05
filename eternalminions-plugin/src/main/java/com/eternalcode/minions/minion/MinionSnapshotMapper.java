package com.eternalcode.minions.minion;

import com.eternalcode.minions.minion.status.MinionStatusTracker;
import com.eternalcode.minions.minion.upgrade.UpgradeKind;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.inventory.ItemStack;

public final class MinionSnapshotMapper {

    private final MinionStatusTracker statuses;

    public MinionSnapshotMapper(MinionStatusTracker statuses) {
        this.statuses = statuses;
    }

    public MinionSnapshot map(Minion minion) {
        ItemStack[] storedItems = minion.storage().snapshot();
        List<ItemStack> storage = new ArrayList<>(storedItems.length);
        for (ItemStack storedItem : storedItems) {
            storage.add(storedItem);
        }

        Map<String, Integer> upgrades = new LinkedHashMap<>();
        for (Map.Entry<UpgradeKind, Integer> entry : minion.upgrades().entries().entrySet()) {
            upgrades.put(entry.getKey().key(), entry.getValue());
        }

        return new MinionSnapshot(
            minion.details(),
            minion.progress().progress(),
            minion.settings().direction(),
            minion.equipment().tool(),
            storage,
            minion.storage().capacity(),
            upgrades,
            minion.chestPosition(),
            this.statuses.status(minion.id()).key()
        );
    }
}
