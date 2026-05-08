package com.chronos.chronos.service;

import com.chronos.chronos.dto.response.AuthResponse;
import com.chronos.chronos.dto.request.LoginRequest;
import com.chronos.chronos.dto.request.RegisterRequest;
import com.chronos.chronos.entity.User;
import com.chronos.chronos.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    // PasswordEncoder is a Spring Security interface — BCryptPasswordEncoder
    // implements it. We declare the interface, Spring injects BCrypt (configured in SecurityConfig)
    private final PasswordEncoder passwordEncoder;

    public AuthResponse register(RegisterRequest request) {
        // Check if email is already taken
        // We throw a clear error so the frontend can show "email already in use"
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(
                    "An account with this email already exists"
            );
        }

        // Build and save the user
        // passwordEncoder.encode() runs BCrypt — takes ~100ms intentionally
        // Slow hashing makes brute-force attacks impractical
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                // NEVER store the raw password — always store the BCrypt hash
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();

        User savedUser = userRepository.save(user);
        log.info("New user registered: {}", savedUser.getEmail());

        // Generate JWT immediately — user is logged in after registration
        // No need for them to log in again after registering
        String token = jwtService.generateToken(savedUser);
        return AuthResponse.of(token, savedUser.getName(), savedUser.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        // Find user by email — throw if not found
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        // We say "invalid credentials" not "user not found"
                        // This prevents attackers from knowing which emails are registered
                        new BadCredentialsException("Invalid email or password")
                );

        // Check if this is an OAuth user trying to use password login
        if ("OAUTH2_USER_NO_PASSWORD".equals(user.getPasswordHash())) {
            throw new BadCredentialsException(
                    "This account uses Google/GitHub login. Please use OAuth."
            );
        }

        // BCrypt matches: takes the raw password + the stored hash and compares
        // Returns true if they match, false otherwise
        // This is safe — BCrypt was designed so you can't reverse-engineer the password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Failed login attempt for email: {}", request.getEmail());
            throw new BadCredentialsException("Invalid email or password");
        }

        log.info("User logged in: {}", user.getEmail());
        String token = jwtService.generateToken(user);
        return AuthResponse.of(token, user.getName(), user.getEmail());
    }

    public AuthResponse refreshToken(String refreshToken) {
        String email = jwtService.extractEmail(refreshToken);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new BadCredentialsException("Invalid refresh token"));

        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new BadCredentialsException("Refresh token expired or invalid");
        }

        String newToken = jwtService.generateToken(user);
        return AuthResponse.of(newToken, user.getName(), user.getEmail());
    }
}
