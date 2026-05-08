package com.chronos.chronos.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

// This is what we send back to the client after successful login/register
// The client stores this token and sends it in every future request
@Data
@Builder
@AllArgsConstructor
public class AuthResponse {

    // The JWT token string — looks like: xxxxx.yyyyy.zzzzz
    private String token;

    // Token type is always "Bearer" for JWT
    // Client sends: Authorization: Bearer xxxxx.yyyyy.zzzzz
    private String tokenType;

    // User info — helpful so frontend doesn't need to decode the JWT
    private String name;
    private String email;

    // Convenience factory method — cleaner than using the builder everywhere
    public static AuthResponse of(String token, String name, String email) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .name(name)
                .email(email)
                .build();
    }
}
