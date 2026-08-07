package com.eternalcode.minions.database;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Every {@link DatabaseDriverType} advertised in the config (and its comment listing valid
 * values) must have its JDBC driver class actually present on the runtime classpath, or
 * {@link DatabaseManager#connect()} fails with a ClassNotFoundException the moment an admin
 * picks that value in database.yml. This caught MYSQL shipping with no mysql-connector-j
 * dependency at all - see build.gradle.kts.
 */
class DatabaseDriverTypeAvailabilityTest {

    @ParameterizedTest
    @EnumSource(DatabaseDriverType.class)
    void shouldHaveItsJdbcDriverClassOnTheRuntimeClasspath(DatabaseDriverType type) {
        assertThatCode(() -> Class.forName(type.getDriver()))
                .as("driver class for %s must be bundled as a dependency", type)
                .doesNotThrowAnyException();
    }
}
