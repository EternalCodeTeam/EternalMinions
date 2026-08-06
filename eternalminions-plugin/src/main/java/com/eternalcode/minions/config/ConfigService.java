package com.eternalcode.minions.config;

import com.eternalcode.multification.notice.resolver.NoticeResolverDefaults;
import com.eternalcode.multification.okaeri.MultificationSerdesPack;
import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.serdes.commons.SerdesCommons;
import eu.okaeri.configs.yaml.bukkit.serdes.SerdesBukkit;
import eu.okaeri.configs.yaml.snakeyaml.YamlSnakeYamlConfigurer;
import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ConfigService {

    private final Path dataDirectory;
    private final Map<Class<? extends ConfigurationFile>, ConfigurationFile> configs = new LinkedHashMap<>();

    public ConfigService(Path dataDirectory, List<Class<? extends ConfigurationFile>> configTypes) {
        this.dataDirectory = dataDirectory;
        for (Class<? extends ConfigurationFile> configType : List.copyOf(configTypes)) {
            this.load(configType);
        }
    }

    public <T extends ConfigurationFile> T get(Class<T> configType) {
        ConfigurationFile config = this.configs.get(configType);
        if (config == null) {
            throw new IllegalArgumentException("Config is not registered: " + configType.getName());
        }
        return configType.cast(config);
    }

    public void reload() {
        for (ConfigurationFile config : this.configs.values()) {
            config.load(true);
        }
    }

    private <T extends ConfigurationFile> void load(Class<T> configType) {
        if (this.configs.containsKey(configType)) {
            throw new IllegalArgumentException("Config is already registered: " + configType.getName());
        }

        T config = this.configure(ConfigManager.create(configType));
        File file = config.resolve(this.dataDirectory).toFile();
        this.createDirectory(file.getParentFile());
        config.withBindFile(file);
        config.withRemoveOrphans(true);
        config.saveDefaults();
        config.load(true);
        this.configs.put(configType, config);
    }

    private void createDirectory(File directory) {
        if (directory.isDirectory()) {
            return;
        }
        if (!directory.mkdirs()) {
            throw new IllegalStateException("Failed to create config directory: " + directory);
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

}
