package com.eternalcode.minions.config;

import com.eternalcode.multification.notice.resolver.NoticeResolverDefaults;
import com.eternalcode.multification.okaeri.MultificationSerdesPack;
import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.serdes.OkaeriSerdesPack;
import eu.okaeri.configs.serdes.SerdesRegistry;
import eu.okaeri.configs.serdes.commons.SerdesCommons;
import eu.okaeri.configs.yaml.bukkit.serdes.SerdesBukkit;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class ConfigService {

    private final List<ConfigRegistration<?>> configs = new ArrayList<>();

    public <T extends OkaeriConfig> T create(Class<T> configType, File file) {
        T config = this.configure(ConfigManager.create(configType));
        config.withBindFile(file);
        config.withRemoveOrphans(true);
        config.saveDefaults();
        config.load(true);
        this.validate(config);
        this.configs.add(new ConfigRegistration<>(config, configType, file));
        return config;
    }

    public void reload() {
        List<OkaeriConfig> loadedConfigs = new ArrayList<>(this.configs.size());
        for (ConfigRegistration<?> registration : this.configs) {
            loadedConfigs.add(this.loadValidated(registration));
        }

        for (int index = 0; index < this.configs.size(); index++) {
            this.configs.get(index).config().load(loadedConfigs.get(index));
        }
    }

    private <T extends OkaeriConfig> T configure(T config) {
        config.withConfigurer(
            new YamlSnakeYamlConfigurer(),
            new SerdesCommons(),
            new SerdesBukkit(),
            new MultificationSerdesPack(NoticeResolverDefaults.createRegistry()),
            new EternalMinionsSerdesPack()
        );
        return config;
    }

    private <T extends OkaeriConfig> T loadValidated(ConfigRegistration<T> registration) {
        T loaded = this.configure(ConfigManager.create(registration.type()));
        loaded.load(registration.file());
        this.validate(loaded);
        return loaded;
    }

    private void validate(OkaeriConfig config) {
        if (config instanceof MinionPanelConfig panelConfig) {
            MinionPanelLayout.from(panelConfig);
        }
    }

    private static final class EternalMinionsSerdesPack implements OkaeriSerdesPack {

        @Override
        public void register(SerdesRegistry registry) {
            registry.register(new XMaterialTransformer());
        }
    }

    private record ConfigRegistration<T extends OkaeriConfig>(T config, Class<T> type, File file) {
    }
}
