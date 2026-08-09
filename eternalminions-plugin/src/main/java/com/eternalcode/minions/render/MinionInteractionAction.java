package com.eternalcode.minions.render;

public enum MinionInteractionAction {

    PICK_UP,
    ROTATE,
    OPEN_PANEL;

    public static MinionInteractionAction resolve(
            boolean attack,
            boolean sneaking
    ) {
        if (attack) {
            return PICK_UP;
        }
        if (sneaking) {
            return ROTATE;
        }
        return OPEN_PANEL;
    }
}
