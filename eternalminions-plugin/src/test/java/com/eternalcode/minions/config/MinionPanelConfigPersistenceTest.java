package com.eternalcode.minions.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.cryptomorin.xseries.XMaterial;
import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.serdes.commons.SerdesCommons;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import org.junit.jupiter.api.Test;

class MinionPanelConfigPersistenceTest {

    @Test
    void persistsAndLoadsXMaterialAsReadableYamlName() {
        MinionPanelConfig config = ConfigManager.create(MinionPanelConfig.class);
        config.withConfigurer(
            new YamlSnakeYamlConfigurer(),
            new SerdesCommons(),
            registry -> {
                registry.register(new XMaterialTransformer());
                registry.register(new UpgradeKindTransformer());
            }
        );
        String yaml = config.saveToString();
        MinionPanelConfig loaded = ConfigManager.create(MinionPanelConfig.class);
        loaded.withConfigurer(
            new YamlSnakeYamlConfigurer(),
            new SerdesCommons(),
            registry -> {
                registry.register(new XMaterialTransformer());
                registry.register(new UpgradeKindTransformer());
            }
        );
        loaded.load(yaml);

        assertThat(yaml).contains("material: BLACK_STAINED_GLASS_PANE");
        assertThat(yaml).doesNotContain("rendererType", "SELECT_RENDERER", "{MINION_RENDERER}");
        assertThat(loaded.elements.get('#').material).isEqualTo(XMaterial.BLACK_STAINED_GLASS_PANE);
    }
}
