package com.eternalcode.minions.database;

import com.j256.ormlite.jdbc.DataSourceConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.sql.SQLException;

public final class MinionDatabase implements AutoCloseable {

    private final DatabaseConfig config;
    private final File dataFolder;
    private HikariDataSource dataSource;
    private ConnectionSource connectionSource;

    public MinionDatabase(DatabaseConfig config, File dataFolder) {
        this.config = config;
        this.dataFolder = dataFolder;
    }

    public void connect() throws SQLException {
        String jdbcUrl = this.config.databaseType.jdbcUrl(this.config, this.dataFolder);

        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("EternalMinions-Database");
        hikari.setDriverClassName(this.config.databaseType.driver());
        hikari.setJdbcUrl(jdbcUrl);
        hikari.setUsername(this.config.username);
        hikari.setPassword(this.config.password);
        hikari.setMaximumPoolSize(this.config.maximumPoolSize);
        hikari.setConnectionTimeout(this.config.connectionTimeoutMillis);
        hikari.setAutoCommit(true);
        hikari.addDataSourceProperty("cachePrepStmts", "true");
        hikari.addDataSourceProperty("prepStmtCacheSize", "250");
        hikari.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        this.dataSource = new HikariDataSource(hikari);
        this.connectionSource = new DataSourceConnectionSource(this.dataSource, jdbcUrl);
    }

    public ConnectionSource connectionSource() {
        if (this.connectionSource == null) {
            throw new IllegalStateException("Database is not connected");
        }
        return this.connectionSource;
    }

    @Override
    public void close() throws Exception {
        if (this.connectionSource != null) {
            this.connectionSource.close();
        }
        if (this.dataSource != null) {
            this.dataSource.close();
        }
    }
}
