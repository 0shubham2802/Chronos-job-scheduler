package com.chronos.chronos.service;

import com.chronos.chronos.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// No @SpringBootTest here — this is a pure unit test
// JwtService has no DB dependency so we can test it alone
// These tests run in milliseconds — no Spring context needed
class JwtServiceTest {

    private JwtService jwtService;
    private User testUser;

    // @BeforeEach runs before every single test method
    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        // ReflectionTestUtils injects @Value fields in tests
        // since @Value only works when Spring is running
        ReflectionTestUtils.setField(jwtService, "secret",
                "test-secret-key-that-is-long-enough-for-hmac-sha256-algorithm");
        ReflectionTestUtils.setField(jwtService, "expiration", 86400000L);

        // Build a fake user for testing — no DB needed
        testUser = User.builder()
                .id(UUID.randomUUID())
                .name("Shubham Pant")
                .email("shubham@chronos.com")
                .passwordHash("hashed")
                .build();
    }

    @Test
    void shouldGenerateToken() {
        String token = jwtService.generateToken(testUser);

        // JWT format is always three parts separated by dots
        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void shouldExtractCorrectEmailFromToken() {
        String token = jwtService.generateToken(testUser);
        String email = jwtService.extractEmail(token);

        assertThat(email).isEqualTo("shubham@chronos.com");
    }

    @Test
    void shouldValidateTokenForCorrectUser() {
        String token = jwtService.generateToken(testUser);

        assertThat(jwtService.isTokenValid(token, testUser)).isTrue();
    }

    @Test
    void shouldRejectTokenForWrongUser() {
        String token = jwtService.generateToken(testUser);

        // Different user — same token should not be valid
        User anotherUser = User.builder()
                .id(UUID.randomUUID())
                .email("other@chronos.com")
                .name("Other User")
                .passwordHash("hashed")
                .build();

        assertThat(jwtService.isTokenValid(token, anotherUser)).isFalse();
    }

    @Test
    void shouldDetectExpiredToken() {
        // Set expiration to -1000ms so token is already expired when created
        ReflectionTestUtils.setField(jwtService, "expiration", -1000L);
        String expiredToken = jwtService.generateToken(testUser);

        // When a token is expired, extractAllClaims throws ExpiredJwtException
        // isTokenExpired() calls extractAllClaims internally — so we need to
        // catch that exception and treat it as "yes this token is expired"
        // We verify that an exception IS thrown — that's proof it's expired
        assertThatThrownBy(() -> jwtService.isTokenExpired(expiredToken))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test
    void shouldThrowForTamperedToken() {
        String token = jwtService.generateToken(testUser);
        // Tamper with the signature part (last section after the second dot)
        String tamperedToken = token.substring(0, token.lastIndexOf('.'))
                + ".invalidsignature";

        // Any claim extraction on a tampered token should throw
        assertThatThrownBy(() -> jwtService.extractEmail(tamperedToken))
                .isInstanceOf(Exception.class);
    }
}