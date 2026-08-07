package com.eternalcode.minions.database;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class MinionRepositoryPostgresIntegrationTest extends AbstractMinionRepositoryContractTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("eternal_minions")
                    .withUsername("eternal_minions")
                    .withPassword("eternal_minions");

    @Override
    protected DatabaseSettings settings() {
        return new DatabaseSettings() {
            @Override
            public DatabaseDriverType databaseType() {
                return DatabaseDriverType.POSTGRESQL;
            }

            @Override
            public String hostname() {
                return POSTGRES.getHost();
            }

            @Override
            public int port() {
                return POSTGRES.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT);
            }

            @Override
            public String database() {
                return POSTGRES.getDatabaseName();
            }

            @Override
            public String username() {
                return POSTGRES.getUsername();
            }

            @Override
            public String password() {
                return POSTGRES.getPassword();
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
