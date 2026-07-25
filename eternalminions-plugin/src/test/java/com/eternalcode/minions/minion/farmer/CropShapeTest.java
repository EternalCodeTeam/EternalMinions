package com.eternalcode.minions.minion.farmer;

import static org.assertj.core.api.Assertions.assertThat;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class CropShapeTest {

    @Test
    void classifiesSugarCaneAndBambooAsStackingColumn() {
        assertThat(CropShape.of(Material.SUGAR_CANE)).isEqualTo(CropShape.STACKING_COLUMN);
        assertThat(CropShape.of(Material.BAMBOO)).isEqualTo(CropShape.STACKING_COLUMN);
    }

    @Test
    void classifiesPumpkinAndMelonAsAdjacentStemFruit() {
        assertThat(CropShape.of(Material.PUMPKIN)).isEqualTo(CropShape.ADJACENT_STEM_FRUIT);
        assertThat(CropShape.of(Material.MELON)).isEqualTo(CropShape.ADJACENT_STEM_FRUIT);
    }

    @Test
    void classifiesOrdinaryCropsAsAgeableReplant() {
        assertThat(CropShape.of(Material.WHEAT)).isEqualTo(CropShape.AGEABLE_REPLANT);
        assertThat(CropShape.of(Material.CARROTS)).isEqualTo(CropShape.AGEABLE_REPLANT);
        assertThat(CropShape.of(Material.POTATOES)).isEqualTo(CropShape.AGEABLE_REPLANT);
    }
}
