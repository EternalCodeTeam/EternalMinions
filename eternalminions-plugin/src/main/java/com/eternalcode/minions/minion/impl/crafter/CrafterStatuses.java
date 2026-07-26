package com.eternalcode.minions.minion.impl.crafter;

import com.eternalcode.minions.minion.status.MinionStatus;

public final class CrafterStatuses {

    public static final MinionStatus CRAFTING = new MinionStatus("CRAFTING");
    public static final MinionStatus NO_RECIPE_SELECTED = new MinionStatus("NO_RECIPE_SELECTED");
    public static final MinionStatus NO_CHEST = new MinionStatus("NO_CHEST");
    public static final MinionStatus NO_INGREDIENTS = new MinionStatus("NO_INGREDIENTS");
    public static final MinionStatus NO_ROOM_FOR_RESULT = new MinionStatus("NO_ROOM_FOR_RESULT");

    private CrafterStatuses() {
    }
}
