package com.chronos.chronos.security;

import com.chronos.chronos.entity.User;
import com.chronos.chronos.repository.UserRepository;
import com.chronos.chronos.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

// This runs AFTER Google/GitHub successfully authenticates the user
// Our job: save them if new, then generate our own JWT
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler
        extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    // Frontend URL — where to redirect after successful OAuth
    // In production this would be your actual domain
    private static final String FRONTEND_URL = "http://localhost:5173";

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        // Spring Security gives us the OAuth user's profile
        // Google/GitHub already verified this user's identity
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // Extract user info from OAuth provider
        // Google provides "email" and "name" — GitHub provides "email" and "name" too
        String email = oAuth2User.getAttribute("email");
        String name  = oAuth2User.getAttribute("name");

        log.info("OAuth2 login successful for email: {}", email);

        // Check if this user already exists in our DB
        // If they registered before with Google they'll already be here
        // If it's their first time we create a new account for them
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    log.info("Creating new user from OAuth2 login: {}", email);
                    return userRepository.save(
                            User.builder()
                                    .email(email)
                                    .name(name != null ? name : email)
                                    // OAuth users never use a password
                                    // We store a placeholder that can never match any BCrypt hash
                                    .passwordHash("OAUTH2_USER_NO_PASSWORD")
                                    .build()
                    );
                });

        // Generate our standard JWT — same as email/password login
        // From here the frontend works identically regardless of login method
        String jwt = jwtService.generateToken(user);

        // Redirect to frontend's callback page with the token as a URL parameter
        // Frontend reads it from the URL and stores in localStorage
        String redirectUrl = FRONTEND_URL + "/auth/callback?token=" + jwt
                + "&name=" + encodeParam(user.getName())
                + "&email=" + encodeParam(user.getEmail());

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private String encodeParam(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }
}
