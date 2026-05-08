package com.chronos.chronos;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for all integration tests that need a full Spring context.
 *
 * Uses the Testcontainers "singleton pattern" — containers are started ONCE
 * as static fields in this class and kept alive until the JVM exits.
 * This avoids the per-class lifecycle issues with @Testcontainers/@Container
 * annotations in base classes, and ensures all test classes share the same
 * container instances (and therefore the same Spring context via caching).
 *
 * Quartz is forced to in-memory mode so it never needs its own DB tables,
 * and auto-startup is disabled so no triggers fire during tests.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.quartz.job-store-type=memory",
                "spring.quartz.properties.org.quartz.jobStore.isClustered=false",
                "spring.quartz.auto-startup=false"
        }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    // ── PostgreSQL singleton container ────────────────────────────────────────
    // 'static final' + manual start() means it starts once and is reused
    // across ALL test classes. withReuse(true) keeps it alive between Gradle runs
    // (Testcontainers reuse feature — requires ~/.testcontainers.properties with
    // testcontainers.reuse.enable=true).
    static final PostgreSQLContainer<?> POSTGRES;

    // ── Redis singleton container ─────────────────────────────────────────────
    static final RedisContainer REDIS;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
                .withDatabaseName("chronos_test")
                .withUsername("postgres")
                .withPassword("postgres")
                .withReuse(true);
        POSTGRES.start();

        REDIS = new RedisContainer(DockerImageName.parse("redis:7-alpine"))
                .withReuse(true);
        REDIS.start();
    }

    // ── Wire dynamic container ports into Spring's environment ─────────────────
    // Spring calls this before creating the ApplicationContext so the DataSource
    // and Redis connection factory get the correct Testcontainer host/port.
    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url",      POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name",
                () -> "org.postgresql.Driver");

        // Flyway must use the same URL
        registry.add("spring.flyway.url",      POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user",     POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);

        // Redis
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379).toString());
    }
}
