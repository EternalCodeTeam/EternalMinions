package com.eternalcode.minions.database;

import java.io.File;

public enum DatabaseType {
    H2("org.h2.Driver"),
    MYSQL("org.mariadb.jdbc.Driver"),
    MARIADB("org.mariadb.jdbc.Driver"),
    POSTGRESQL("org.postgresql.Driver");

    private final String driver;

    DatabaseType(String driver) {
        this.driver = driver;
    }

    public String driver() {
        return this.driver;
    }

    public String jdbcUrl(DatabaseConfig config, File dataFolder) {
        return switch (this) {
            case H2 -> "jdbc:h2:file:" + new File(dataFolder, "minions").getAbsolutePath() + ";MODE=MySQL";
            case MYSQL -> "jdbc:mariadb://" + config.hostname + ':' + config.port + '/' + config.databaseName
                + "?useSsl=" + config.useSsl;
            case MARIADB -> "jdbc:mariadb://" + config.hostname + ':' + config.port + '/' + config.databaseName
                + "?useSsl=" + config.useSsl;
            case POSTGRESQL -> "jdbc:postgresql://" + config.hostname + ':' + config.port + '/' + config.databaseName
                + "?ssl=" + config.useSsl;
        };
    }
}
