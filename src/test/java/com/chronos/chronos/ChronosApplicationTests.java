package com.chronos.chronos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Extends IntegrationTestBase — PostgreSQL + Redis containers are started
// automatically, and the Spring context is loaded with the correct properties.
class ChronosApplicationTests extends IntegrationTestBase {

    @Test
    void contextLoads() {
        // If the Spring context loads without throwing, this test passes.
        // It validates that all beans wire together correctly.
    }
}