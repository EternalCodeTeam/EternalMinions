package com.eternalcode.minions.database;

import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class MinionRepositoryMySqlIntegrationTest extends AbstractMinionRepositoryContractTest {

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4")
                    .withDatabaseName("eternal_minions")
                    .withUsername("eternal_minions")
                    .withPassword("eternal_minions");

    @Override
    protected DatabaseSettings settings() {
        return new DatabaseSettings() {
            @Override
            public DatabaseDriverType databaseType() {
                return DatabaseDriverType.MYSQL;
            }

            @Override
            public String hostname() {
                return MYSQL.getHost();
            }

            @Override
            public int port() {
                return MYSQL.getMappedPort(MySQLContainer.MYSQL_PORT);
            }

            @Override
            public String database() {
                return MYSQL.getDatabaseName();
            }

            @Override
            public String username() {
                return MYSQL.getUsername();
            }

            @Override
            public String password() {
                return MYSQL.getPassword();
            }

            @Override
            public boolean ssl() {
                return false;
            }

            @Override
            public int poolSize() {
                return 4;
            }

            @Override
            public int timeout() {
                return 10_000;
            }
        };
    }
}
