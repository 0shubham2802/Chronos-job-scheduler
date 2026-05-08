package com.chronos.chronos.security;

import com.chronos.chronos.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// UserDetailsService is a Spring Security interface
// Spring calls loadUserByUsername() when it needs to verify a user
// "username" in Spring Security = email in our case
@Service
@RequiredArgsConstructor  // Lombok generates constructor with all final fields
public class CustomUserDetailsService implements UserDetailsService {

    // @RequiredArgsConstructor sees this final field and injects it
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {
        // Try to find the user by email
        // If not found, throw UsernameNotFoundException
        // Spring Security catches this and returns 401 automatically
        return userRepository.findByEmail(email)
                .map(user ->
                        // Spring Security's User class implements UserDetails
                        // We map our User entity to Spring's User object
                        org.springframework.security.core.userdetails.User
                                .withUsername(user.getEmail())
                                // The stored BCrypt hash — Spring verifies this
                                .password(user.getPasswordHash())
                                // Roles/permissions — we'll expand this later
                                // For now everyone has ROLE_USER
                                .roles("USER")
                                .build()
                )
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found with email: " + email
                        )
                );
    }
}