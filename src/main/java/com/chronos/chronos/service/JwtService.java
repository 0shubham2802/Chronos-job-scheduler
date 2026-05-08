package com.chronos.chronos.service;

import com.chronos.chronos.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

// @Service tells Spring this is a service class — it gets registered
// as a Spring Bean and can be injected with @Autowired or constructor injection
@Service
public class JwtService {

    // @Value reads from application.properties
    // The ${jwt.secret} pulls the value we set there
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    // Converts our secret string into a cryptographic key
    // HMAC-SHA256 (HS256) is the signing algorithm we use
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // Generates a JWT token for the given user
    // This is called after successful login OR OAuth callback
    public String generateToken(User user) {
        return Jwts.builder()
                // "sub" (subject) = who this token is about = the user's email
                .subject(user.getEmail())
                // Custom claims = extra data we embed in the token
                // The client can decode these without hitting the DB
                .claim("userId", user.getId().toString())
                .claim("name", user.getName())
                // "iat" (issued at) = when this token was created
                .issuedAt(new Date())
                // "exp" (expiration) = when this token stops working
                // System.currentTimeMillis() + expiration = now + 24 hours
                .expiration(new Date(System.currentTimeMillis() + expiration))
                // Sign the token with our secret key
                // This is what prevents anyone from tampering with the token
                .signWith(getSigningKey())
                // Build it into the final JWT string: xxxxx.yyyyy.zzzzz
                .compact();
    }

    // Extracts all claims (data) from a token
    // "claims" is what's stored inside the token's payload section
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                // Provide the same key we used to sign — if the token was
                // tampered with, the signature won't match and an exception is thrown
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Pulls the email (subject) out of the token
    // We use this to look up the user in the database
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    // Checks if the token has expired
    public boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    // Full token validation:
    // 1. Is the email in the token the same as the user we loaded from DB?
    // 2. Has the token expired?
    public boolean isTokenValid(String token, User user) {
        try {
            final String email = extractEmail(token);
            return email.equals(user.getEmail()) && !isTokenExpired(token);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            // Token is expired — clearly not valid
            return false;
        } catch (Exception e) {
            // Token is malformed or tampered — not valid
            return false;
        }
    }
}
