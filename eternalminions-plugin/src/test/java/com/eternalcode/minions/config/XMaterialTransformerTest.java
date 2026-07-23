package com.eternalcode.minions.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.cryptomorin.xseries.XMaterial;
import org.junit.jupiter.api.Test;

class XMaterialTransformerTest {

    private final XMaterialTransformer transformer = new XMaterialTransformer();

    @Test
    void transformsConfiguredNameToXMaterial() {
        assertThat(this.transformer.leftToRight("diamond_pickaxe", null)).isEqualTo(XMaterial.DIAMOND_PICKAXE);
    }

    @Test
    void serializesXMaterialUsingCanonicalName() {
        assertThat(this.transformer.rightToLeft(XMaterial.PLAYER_HEAD, null)).isEqualTo("PLAYER_HEAD");
    }

    @Test
    void rejectsUnknownMaterial() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> this.transformer.leftToRight("NOT_A_REAL_MATERIAL", null))
            .withMessage("Unknown XMaterial: NOT_A_REAL_MATERIAL");
    }
}
