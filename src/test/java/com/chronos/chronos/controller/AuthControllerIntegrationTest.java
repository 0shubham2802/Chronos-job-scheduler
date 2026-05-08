package com.chronos.chronos.controller;

import com.chronos.chronos.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Extends IntegrationTestBase — full Spring context with real PostgreSQL + Redis
// containers. MockMvc is auto-configured by the base class (@AutoConfigureMockMvc).
class AuthControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    private String uniqueEmail(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 8) + "@chronos.com";
    }

    @Test
    void register_ShouldReturn201AndToken() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "name": "Shubham Pant",
                        "email": "%s",
                        "password": "password123"
                    }
                    """.formatted(uniqueEmail("register"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void register_ShouldReturn400OnDuplicateEmail() throws Exception {
        String email = uniqueEmail("duplicate");
        String body = """
            {
                "name": "Shubham Pant",
                "email": "%s",
                "password": "password123"
            }
            """.formatted(email);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_ShouldReturn400OnInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "name": "Shubham",
                        "email": "not-an-email",
                        "password": "password123"
                    }
                    """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_ShouldReturn200AndToken() throws Exception {
        String email = uniqueEmail("login");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "name": "Shubham Pant",
                        "email": "%s",
                        "password": "password123"
                    }
                    """.formatted(email)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "email": "%s",
                        "password": "password123"
                    }
                    """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void login_ShouldReturn401OnWrongPassword() throws Exception {
        String email = uniqueEmail("wrongpw");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "name": "Shubham Pant",
                        "email": "%s",
                        "password": "correctpassword"
                    }
                    """.formatted(email)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "email": "%s",
                        "password": "wrongpassword"
                    }
                    """.formatted(email)))
                .andExpect(status().isUnauthorized());
    }
}