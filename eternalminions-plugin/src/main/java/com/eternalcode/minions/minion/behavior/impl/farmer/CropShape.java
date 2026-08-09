package com.eternalcode.minions.minion.behavior.impl.farmer;

public enum CropShape {

    // Harvests a fully grown ageable crop and resets its age to zero.
    AGEABLE_REPLANT,

    // Harvests only the upper blocks while preserving the bottom block.
    STACKING_COLUMN,

    // Harvests fruit blocks produced next to a persistent stem.
    STEM_FRUIT
}