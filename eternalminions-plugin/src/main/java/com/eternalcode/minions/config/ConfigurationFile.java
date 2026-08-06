package com.eternalcode.minions.config;

import eu.okaeri.configs.OkaeriConfig;
import java.nio.file.Path;

public abstract class ConfigurationFile extends OkaeriConfig {

    public abstract Path resolve(Path dataDirectory);
}
