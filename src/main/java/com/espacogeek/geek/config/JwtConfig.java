package com.espacogeek.geek.config;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.espacogeek.geek.models.UserModel;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import jakarta.servlet.http.HttpServletRequest;

/**
 * JWT configuration and helper methods for token generation and validation.
 */
@Component
public class JwtConfig {

    @Value("${security.jwt.secret:ZmFrZS1zZWNyZXQtZmFrZS1zZWNyZXQtZmFrZS1zZWNyZXQtMTIzNDU2}")
    private String secret;

    @Value("${security.jwt.expiration-ms:604800000}")
    private long expirationMs;

    @Value("${security.jwt.issuer:espaco-geek}")
    private String issuer;

    // Cookie settings
    @Value("${security.jwt.cookie-name:EG_AUTH}")
    private String cookieName;

    @Value("${security.jwt.cookie-path:/}")
    private String cookiePath;

    @Value("${security.jwt.cookie-domain:}")
    private String cookieDomain;

    // When request is same-site, which SameSite to use (Lax or Strict). Default Lax.
    @Value("${security.jwt.same-site-when-same-site:Lax}")
    private String sameSiteWhenSameSite;

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

        // Build roles list from user.userRole (comma separated) and ensure ID_ claim is included
        List<String> rolesList = new ArrayList<>();
        String raw = user.getUserRole();
        if (raw != null && !raw.isBlank()) {
            String[] parts = raw.replaceAll("\\s", "").split(",");
            // Normalize roles: if a role doesn't start with ROLE_ or ID_, prefix with ROLE_
            rolesList.addAll(Arrays.stream(parts)
                    .map(s -> s == null ? null : s.trim())
                    .filter(s -> s != null && !s.isBlank())
                    .map(s -> {
                        if (s.startsWith("ROLE_") || s.startsWith("ID_")) return s;
                        return "ROLE_" + s;
                    })
                    .toList());
        }
        // Ensure at least ROLE_user is present as a fallback
        if (rolesList.isEmpty()) {
            rolesList.add("ROLE_user");
        }
        // Add device/user identifier role (keep ID_ as-is)
        rolesList.add("ID_" + user.getId());

        return Jwts.builder()
                .issuer(issuer)
                .subject(user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .claim("uid", user.getId())
                .claim("roles", rolesList)
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

    /**
     * Get the name of the auth cookie for clients.
     */
    public String cookieName() {
        return cookieName;
    }

    /**
     * Build the Set-Cookie for the auth token with HttpOnly; Secure; Path=/ and appropriate SameSite.
     * - If different site (domain/port/scheme) from backend: SameSite=None; Secure
     * - If same site: SameSite=Lax/Strict based on configuration
     */
    public ResponseCookie buildAuthCookie(String token, HttpServletRequest request) {
        boolean crossSite = isCrossSite(request);
        String sameSite = crossSite ? "None" : normalizeSameSite(sameSiteWhenSameSite);
        boolean secure = crossSite || request.isSecure();

        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(cookieName, token)
                .httpOnly(true)
                .secure(secure)
                .path(cookiePath)
                .maxAge(Duration.ofMillis(expirationMs))
                .sameSite(sameSite);

        if (cookieDomain != null && !cookieDomain.isBlank()) {
            builder.domain(cookieDomain);
        }
        return builder.build();
    }

    /** Build auth cookie using Origin header and server URI (for GraphQL interceptor). */
    public ResponseCookie buildAuthCookie(String token, String originHeader, URI serverUri) {
        boolean crossSite = isCrossSite(originHeader, serverUri);
        String sameSite = crossSite ? "None" : normalizeSameSite(sameSiteWhenSameSite);
        boolean secure = crossSite || "https".equalsIgnoreCase(serverUri.getScheme());

        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(cookieName, token)
                .httpOnly(true)
                .secure(secure)
                .path(cookiePath)
                .maxAge(Duration.ofMillis(expirationMs))
                .sameSite(sameSite);
        if (cookieDomain != null && !cookieDomain.isBlank()) {
            builder.domain(cookieDomain);
        }
        return builder.build();
    }

    /**
     * Build a Set-Cookie header that clears the auth cookie.
     */
    public ResponseCookie clearAuthCookie(HttpServletRequest request) {
        boolean crossSite = isCrossSite(request);
        String sameSite = crossSite ? "None" : normalizeSameSite(sameSiteWhenSameSite);
        boolean secure = crossSite || request.isSecure();

        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(secure)
                .path(cookiePath)
                .maxAge(Duration.ZERO)
                .sameSite(sameSite);
        if (cookieDomain != null && !cookieDomain.isBlank()) {
            builder.domain(cookieDomain);
        }
        return builder.build();
    }

    /** Clear auth cookie using Origin header and server URI (for GraphQL interceptor). */
    public ResponseCookie clearAuthCookie(String originHeader, URI serverUri) {
        boolean crossSite = isCrossSite(originHeader, serverUri);
        String sameSite = crossSite ? "None" : normalizeSameSite(sameSiteWhenSameSite);
        boolean secure = crossSite || "https".equalsIgnoreCase(serverUri.getScheme());

        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(secure)
                .path(cookiePath)
                .maxAge(Duration.ZERO)
                .sameSite(sameSite);
        if (cookieDomain != null && !cookieDomain.isBlank()) {
            builder.domain(cookieDomain);
        }
        return builder.build();
    }

    private static String normalizeSameSite(String value) {
        if (value == null) return "Lax";
        String v = value.trim();
        if (v.equalsIgnoreCase("Strict")) return "Strict";
        if (v.equalsIgnoreCase("None")) return "None";
        return "Lax"; // default
    }

    private boolean isCrossSite(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin == null || origin.isBlank()) {
            // No Origin header (e.g., same-origin navigations); treat as same-site
            return false;
        }
        // Compare only scheme + host (ignore port) per RFC6265bis same-site rules
        try {
            URI originUri = URI.create(origin);
            String originSite = originUri.getScheme() + "://" + originUri.getHost();
            String serverSite = request.getScheme() + "://" + request.getServerName();
            return !originSite.equalsIgnoreCase(serverSite);
        } catch (IllegalArgumentException ex) {
            // Fallback to strict comparison
            String serverOrigin = getServerOrigin(request);
            return !origin.equalsIgnoreCase(serverOrigin);
        }
    }

    private boolean isCrossSite(String originHeader, URI serverUri) {
        if (originHeader == null || originHeader.isBlank()) return false;
        try {
            URI originUri = URI.create(originHeader);
            String originSite = originUri.getScheme() + "://" + originUri.getHost();
            String serverSite = serverUri.getScheme() + "://" + serverUri.getHost();
            return !originSite.equalsIgnoreCase(serverSite);
        } catch (IllegalArgumentException ex) {
            String serverOrigin = getServerOrigin(serverUri);
            return !originHeader.equalsIgnoreCase(serverOrigin);
        }
    }

    private String getServerOrigin(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        boolean isDefaultPort = ("http".equalsIgnoreCase(scheme) && port == 80)
                || ("https".equalsIgnoreCase(scheme) && port == 443);
        return scheme + "://" + host + (isDefaultPort ? "" : (":" + port));
    }

    private String getServerOrigin(URI uri) {
        String scheme = uri.getScheme();
        String host = uri.getHost();
        int port = uri.getPort();
        boolean isDefaultPort = ("http".equalsIgnoreCase(scheme) && (port == -1 || port == 80))
                || ("https".equalsIgnoreCase(scheme) && (port == -1 || port == 443));
        return scheme + "://" + host + (isDefaultPort ? "" : (":" + port));
    }
}
