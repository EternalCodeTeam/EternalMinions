package com.eternalcode.minions.minion.behavior.impl.crafter;

import com.eternalcode.minions.minion.status.MinionStatus;

public final class CrafterStatuses {

    public static final MinionStatus CRAFTING = MinionStatus.of("CRAFTING");
    public static final MinionStatus NO_RECIPE_SELECTED = MinionStatus.of("NO_RECIPE_SELECTED");
    public static final MinionStatus NO_CHEST = MinionStatus.of("NO_CHEST");
    public static final MinionStatus NO_INGREDIENTS = MinionStatus.of("NO_INGREDIENTS");

    private CrafterStatuses() {
    }
}
