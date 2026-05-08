package com.chronos.chronos.service;

import com.chronos.chronos.dto.response.AuthResponse;
import com.chronos.chronos.dto.request.LoginRequest;
import com.chronos.chronos.dto.request.RegisterRequest;
import com.chronos.chronos.entity.User;
import com.chronos.chronos.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class) activates Mockito for this test class
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    // @Mock creates a fake version of these classes
    // They don't actually hit the DB or generate real JWTs
    // We control exactly what they return in each test
    @Mock private UserRepository userRepository;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;

    // @InjectMocks creates a REAL AuthService and injects all the @Mock objects into it
    @InjectMocks private AuthService authService;

    private User savedUser;

    @BeforeEach
    void setUp() {
        savedUser = User.builder()
                .id(UUID.randomUUID())
                .name("Shubham Pant")
                .email("shubham@chronos.com")
                .passwordHash("$2a$12$hashedpassword")
                .build();
    }

    @Test
    void register_ShouldReturnTokenOnSuccess() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Shubham Pant");
        request.setEmail("shubham@chronos.com");
        request.setPassword("password123");

        // "when...thenReturn" = when this mock method is called, return this
        // This is how we control fake behaviour without a real DB
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hashedpassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("fake.jwt.token");

        AuthResponse response = authService.register(request);

        assertThat(response.getToken()).isEqualTo("fake.jwt.token");
        assertThat(response.getEmail()).isEqualTo("shubham@chronos.com");
        assertThat(response.getTokenType()).isEqualTo("Bearer");

        // verify() confirms that save() was called exactly once
        // This ensures we actually tried to save the user, not just return fake data
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_ShouldHashPasswordBeforeSaving() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Shubham Pant");
        request.setEmail("shubham@chronos.com");
        request.setPassword("myrawpassword");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("myrawpassword")).thenReturn("$2a$12$hashed");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("token");

        authService.register(request);

        // Verify password encoder was called with the raw password
        // This proves we NEVER save a plain-text password
        verify(passwordEncoder).encode("myrawpassword");
    }

    @Test
    void register_ShouldThrowWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("shubham@chronos.com");
        request.setPassword("password123");
        request.setName("Shubham");

        // Simulate that this email is already taken
        when(userRepository.existsByEmail("shubham@chronos.com")).thenReturn(true);

        // assertThatThrownBy verifies an exception is thrown
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

        // Verify save was NEVER called — we shouldn't save if email is taken
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_ShouldReturnTokenOnSuccess() {
        LoginRequest request = new LoginRequest();
        request.setEmail("shubham@chronos.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("shubham@chronos.com"))
                .thenReturn(Optional.of(savedUser));
        // passwordEncoder.matches(raw, hashed) returns true = correct password
        when(passwordEncoder.matches("password123", "$2a$12$hashedpassword"))
                .thenReturn(true);
        when(jwtService.generateToken(savedUser)).thenReturn("fake.jwt.token");

        AuthResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("fake.jwt.token");
    }

    @Test
    void login_ShouldThrowOnWrongPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail("shubham@chronos.com");
        request.setPassword("wrongpassword");

        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(savedUser));
        // Simulate password mismatch
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_ShouldThrowWhenUserNotFound() {
        LoginRequest request = new LoginRequest();
        request.setEmail("ghost@chronos.com");
        request.setPassword("password");

        // Simulate user not in DB
        when(userRepository.findByEmail("ghost@chronos.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }
}
