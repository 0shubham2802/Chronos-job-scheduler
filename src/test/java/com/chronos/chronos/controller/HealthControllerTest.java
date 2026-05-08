package com.chronos.chronos.controller;

import com.chronos.chronos.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Extends IntegrationTestBase — needs real PostgreSQL + Redis containers because:
//   GET /api/health      → checks DB connection + Redis ping
//   GET /api/health/ready → calls jobRepository.count() against real DB
class HealthControllerTest extends IntegrationTestBase {

    @Autowired private MockMvc mockMvc;

    @Test
    void health_ShouldReturn200WithStatus() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").isNotEmpty())
                .andExpect(jsonPath("$.database").isNotEmpty())
                .andExpect(jsonPath("$.uptime").isNotEmpty())
                .andExpect(jsonPath("$.version").value("1.0.0"));
    }

    @Test
    void healthReady_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/health/ready"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"));
    }
}
