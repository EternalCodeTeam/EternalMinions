package com.eternalcode.minions.item;

import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionBehaviorRegistry;
import com.eternalcode.minions.minion.MinionId;
import com.eternalcode.minions.minion.MinionRegistry;
import com.eternalcode.minions.minion.upgrade.UpgradeKind;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bukkit.inventory.ItemStack;

public final class MinionItemServiceImpl implements MinionItemService {

    private final MinionItemFactory items;
    private final MinionBehaviorRegistry behaviors;
    private final MinionRegistry minions;

    public MinionItemServiceImpl(
        MinionItemFactory items,
        MinionBehaviorRegistry behaviors,
        MinionRegistry minions
    ) {
        this.items = items;
        this.behaviors = behaviors;
        this.minions = minions;
    }

    @Override
    public ItemStack create(String behaviorId) {
        return this.items.create(this.behaviors.require(behaviorId));
    }

    @Override
    public Optional<ItemStack> createFromMinion(MinionId minionId) {
        return this.minions.findMinion(minionId).map(this.items::create);
    }

    @Override
    public boolean isMinionItem(ItemStack item) {
        return this.items.isMinion(item);
    }

    @Override
    public Optional<MinionItemSnapshot> inspect(ItemStack item) {
        return this.items.readState(item).map(this::map);
    }

    private MinionItemSnapshot map(MinionItemFactory.StoredMinionState state) {
        List<ItemStack> storage = new ArrayList<>(state.storage().length);
        for (ItemStack storedItem : state.storage()) {
            storage.add(storedItem);
        }

        Map<String, Integer> upgrades = new LinkedHashMap<>();
        for (Map.Entry<UpgradeKind, Integer> entry : state.upgrades().entries().entrySet()) {
            upgrades.put(entry.getKey().key(), entry.getValue());
        }

        return new MinionItemSnapshot(
            state.behaviorId(),
            state.level(),
            state.progress(),
            state.tool(),
            storage,
            upgrades
        );
    }
}
