package com.eternalcode.minions.database;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;

public final class DatabaseConfig extends OkaeriConfig {

    @Comment("Database engine used to persist minions. H2 requires no external server.")
    public DatabaseType databaseType = DatabaseType.H2;

    public String hostname = "localhost";
    public int port = 3306;
    public String databaseName = "eternal_minions";
    public String username = "root";
    public String password = "";
    public boolean useSsl = false;

    @Comment("Maximum number of database connections. Minion writes are batched.")
    public int maximumPoolSize = 4;

    @Comment("Maximum time spent waiting for a database connection.")
    public long connectionTimeoutMillis = 10_000L;

    @Comment("How often dirty minions are written in one batch.")
    public long dirtyMinionFlushTicks = 200L;
}
