package com.chronos.chronos.migration;

import com.chronos.chronos.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

// Extends IntegrationTestBase — PostgreSQL Testcontainer is started and Flyway
// migrations are applied automatically before any test method runs.
class FlywayMigrationTest extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void usersTableShouldExist() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE table_name = 'users'", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void jobsTableShouldExist() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE table_name = 'jobs'", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void executionLogsTableShouldExist() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE table_name = 'execution_logs'", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void usersTableShouldHaveCorrectColumns() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_name = 'users' " +
                        "AND column_name IN ('id','name','email','password_hash','created_at')",
                Integer.class);
        assertThat(count).isEqualTo(5);
    }

    @Test
    void jobsTableShouldHaveCorrectColumns() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_name = 'jobs' " +
                        "AND column_name IN ('id','user_id','name','type','status'," +
                        "'payload','cron_expression','scheduled_at','max_retries'," +
                        "'retry_count','timezone')",
                Integer.class);
        assertThat(count).isEqualTo(11);
    }

    @Test
    void shouldBeAbleToInsertAndQueryUser() {
        jdbcTemplate.execute(
                "INSERT INTO users (name, email, password_hash) " +
                        "VALUES ('Test User', 'test_flyway@chronos.com', 'hashed')");
        String email = jdbcTemplate.queryForObject(
                "SELECT email FROM users WHERE name = 'Test User'",
                String.class);
        assertThat(email).isEqualTo("test_flyway@chronos.com");
        jdbcTemplate.execute(
                "DELETE FROM users WHERE name = 'Test User'");
    }
}