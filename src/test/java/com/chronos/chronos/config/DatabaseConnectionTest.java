package com.chronos.chronos.config;

import com.chronos.chronos.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

// Extends IntegrationTestBase — PostgreSQL + Redis Testcontainers are wired in
// automatically. No manual @SpringBootTest / @ActiveProfiles needed here.
class DatabaseConnectionTest extends IntegrationTestBase {

    @Autowired
    private DataSource dataSource;

    @Test
    void shouldConnectToDatabase() {
        assertThatCode(() -> {
            Connection connection = dataSource.getConnection();
            assertThat(connection).isNotNull();
            assertThat(connection.isValid(2)).isTrue();
            connection.close();
        }).doesNotThrowAnyException();
    }

    @Test
    void springContextShouldLoadSuccessfully() {
        assertThat(dataSource).isNotNull();
    }
}