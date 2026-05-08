package com.chronos.chronos.security;

import com.chronos.chronos.entity.User;
import com.chronos.chronos.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityUtils {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null) {
            log.error("Authentication is null in SecurityContextHolder");
            throw new RuntimeException("No authentication found");
        }

        if (!authentication.isAuthenticated()) {
            log.error("User is not authenticated");
            throw new RuntimeException("User is not authenticated");
        }

        String email = authentication.getName();
        log.debug("Looking up user with email: {}", email);

        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User not found in DB with email: {}", email);
                    return new RuntimeException("User not found: " + email);
                });
    }
}