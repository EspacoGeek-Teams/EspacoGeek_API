package com.espacogeek.geek.config;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.espacogeek.geek.models.UserModel;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;

/**
 * JWT configuration and helper methods for token generation and validation.
 */
@Component
public class JwtConfig {

    @Value("${security.jwt.secret:ZmFrZS1zZWNyZXQtZmFrZS1zZWNyZXQtZmFrZS1zZWNyZXQtMTIzNDU2}") // base64 for default
    private String secret;

    @Value("${security.jwt.expiration-ms:3600000}")
    private long expirationMs;

    @Value("${security.jwt.issuer:espaco-geek}")
    private String issuer;

    private SecretKey getSigningKey() {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException ex) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generate a signed JWT for the given user.
     * @param user the authenticated user
     * @return compact JWT string
     */
    public String generateToken(UserModel user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .claim("uid", user.getId())
                .claim("roles", List.of("ROLE_user", "ID_" + user.getId()))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Validate a token and return its claims if valid.
     * @param token JWT string
     * @return claims or null if invalid
     */
    public Claims validate(String token) {
        try {
            var jwt = Jwts.parser()
                    .requireIssuer(issuer)
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return jwt.getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Checks if a token is valid.
     */
    public boolean isValid(String token) {
        return validate(token) != null;
    }
}
