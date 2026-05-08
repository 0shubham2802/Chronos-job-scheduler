package com.chronos.chronos.security;

import com.chronos.chronos.entity.User;
import com.chronos.chronos.repository.UserRepository;
import com.chronos.chronos.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// OncePerRequestFilter guarantees this filter runs exactly once per request
// @Slf4j generates a 'log' variable for logging — provided by Lombok
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Every request that has a JWT sends it in the Authorization header
        // like this: "Authorization: Bearer xxxxx.yyyyy.zzzzz"
        final String authHeader = request.getHeader("Authorization");

        // If there's no Authorization header, or it doesn't start with "Bearer "
        // this request has no JWT — just pass it along to the next filter
        // (Spring Security will block it if the route is protected)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract just the token part — strip the "Bearer " prefix (7 characters)
        final String jwt = authHeader.substring(7);

        try {
            // Extract the email from the token's payload
            final String email = jwtService.extractEmail(jwt);

            // getAuthentication() == null means Spring doesn't know who this is yet
            // We only proceed if email exists AND user isn't already authenticated
            if (email != null &&
                    SecurityContextHolder.getContext().getAuthentication() == null) {

                // Load the full user from the database using the email from token
                User user = userRepository.findByEmail(email).orElse(null);

                // Validate the token against this user
                // (checks email matches + token not expired)
                if (user != null && jwtService.isTokenValid(jwt, user)) {

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    user.getEmail(),  // store email string as principal
                                    user.getId(),     // store user ID as credentials
                                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
                            );

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // If token is invalid/expired/tampered with, log it and move on
            // Spring Security will handle the 401 response
            log.warn("JWT validation failed: {}", e.getMessage());
        }

        // Always continue to the next filter in the chain
        filterChain.doFilter(request, response);
    }
}
