package com.chronos.chronos.controller;

import com.chronos.chronos.IntegrationTestBase;
import com.chronos.chronos.entity.User;
import com.chronos.chronos.repository.JobRepository;
import com.chronos.chronos.repository.UserRepository;
import com.chronos.chronos.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Extends IntegrationTestBase — full Spring context with real PostgreSQL + Redis
// containers. MockMvc is auto-configured by the base class (@AutoConfigureMockMvc).
class JobControllerIntegrationTest extends IntegrationTestBase {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private JobRepository jobRepository;
    @Autowired private JwtService jwtService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ObjectMapper objectMapper;

    private String authToken;
    private User testUser;

    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();

        String email = "test_" + System.currentTimeMillis() + "@chronos.com";

        testUser = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.saveAndFlush(
                        User.builder()
                                .name("Shubham Pant")
                                .email(email)
                                .passwordHash(passwordEncoder.encode("password123"))
                                .build()
                ));

        authToken = "Bearer " + jwtService.generateToken(testUser);
    }

    @Test
    void createJob_ShouldReturn201() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/jobs")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "name": "Send weekly report",
                        "type": "ONE_TIME",
                        "scheduledAt": "2026-12-01T09:00:00",
                        "timezone": "Asia/Kolkata"
                    }
                    """))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(201);

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("Send weekly report");
        assertThat(body).contains("PENDING");
    }

    @Test
    void createJob_ShouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "name": "Test Job",
                        "type": "ONE_TIME",
                        "scheduledAt": "2026-12-01T09:00:00"
                    }
                    """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createJob_ShouldReturn400WhenNameMissing() throws Exception {
        mockMvc.perform(post("/api/jobs")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "type": "ONE_TIME",
                        "scheduledAt": "2026-12-01T09:00:00"
                    }
                    """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.name").isNotEmpty());
    }

    @Test
    void getJobs_ShouldReturnEmptyListInitially() throws Exception {
        mockMvc.perform(get("/api/jobs")
                        .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test
    void getJob_ShouldReturn404ForNonExistentJob() throws Exception {
        mockMvc.perform(get("/api/jobs/" + java.util.UUID.randomUUID())
                        .header("Authorization", authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteJob_ShouldReturn204() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/jobs")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "name": "Job to delete",
                        "type": "ONE_TIME",
                        "scheduledAt": "2026-12-01T09:00:00"
                    }
                    """))
                .andReturn();

        assertThat(createResult.getResponse().getStatus()).isEqualTo(201);

        String jobId = objectMapper
                .readTree(createResult.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(delete("/api/jobs/" + jobId)
                        .header("Authorization", authToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/jobs/" + jobId)
                        .header("Authorization", authToken))
                .andExpect(status().isNotFound());
    }
}