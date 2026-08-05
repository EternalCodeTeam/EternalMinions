package com.eternalcode.minions.render;

enum MinionInteractionAction {

    PICK_UP,
    ROTATE,
    OPEN_PANEL;

    static MinionInteractionAction resolve(
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
