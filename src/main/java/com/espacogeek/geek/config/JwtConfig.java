package com.espacogeek.geek.config;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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

    /** Long-lived refresh token expiry (default 7 days). Used for the refreshToken cookie. */
    @Value("${security.jwt.expiration-ms:604800000}")
    private long expirationMs;

    /** Short-lived access token expiry (default 15 minutes). Returned in the JSON payload. */
    @Value("${security.jwt.access-token-expiration-ms:900000}")
    private long accessTokenExpirationMs;

    @Value("${security.jwt.issuer:espaco-geek}")
    private String issuer;

    // Cookie shared settings (path, domain, SameSite) used for both the refresh token cookie
    // and any other cookie-related operations. The legacy EG_AUTH cookie is no longer used.
    @Value("${security.jwt.cookie-path:/}")
    private String cookiePath;

    @Value("${security.jwt.cookie-domain:}")
    private String cookieDomain;

    // Refresh token cookie settings
    @Value("${security.jwt.refresh-token-cookie-name:refreshToken}")
    private String refreshTokenCookieName;

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
     * Build the normalized roles list for a user.
     */
    private List<String> buildRolesList(UserModel user) {
        List<String> rolesList = new ArrayList<>();
        String raw = user.getUserRole();
        if (raw != null && !raw.isBlank()) {
            String[] parts = raw.replaceAll("\\s", "").split(",");
            rolesList.addAll(Arrays.stream(parts)
                    .filter(s -> !s.isBlank())
                    .map(s -> {
                        if (s.startsWith("ROLE_") || s.startsWith("ID_")) return s;
                        return "ROLE_" + s;
                    })
                    .toList());
        }
        if (rolesList.isEmpty()) {
            rolesList.add("ROLE_user");
        }
        rolesList.add("ID_" + user.getId());
        return rolesList;
    }

    /**
     * Generate a short-lived access token (15 min by default) for the given user.
     * The token carries {@code type=access} and is intended to be returned in the JSON payload.
     *
     * @param user the authenticated user
     * @return compact JWT string
     */
    public String generateAccessToken(UserModel user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessTokenExpirationMs)))
                .claim("uid", user.getId())
                .claim("roles", buildRolesList(user))
                .claim("type", "access")
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Generate a long-lived refresh token (7 days by default) for the given user.
     * The token carries {@code type=refresh} and is stored in the database; it is
     * delivered to the client via an HttpOnly cookie named {@code refreshToken}.
     *
     * @param user the authenticated user
     * @return compact JWT string
     */
    public String generateRefreshToken(UserModel user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .claim("uid", user.getId())
                .claim("roles", buildRolesList(user))
                .claim("type", "refresh")
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Generate a signed JWT access token for the given user.
     * Delegates to {@link #generateAccessToken(UserModel)}.
     * Retained for backward compatibility with test code that calls this method directly.
     *
     * @param user the authenticated user
     * @return compact JWT string
     */
    public String generateToken(UserModel user) {
        return generateAccessToken(user);
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
     * Get the name of the refresh token HttpOnly cookie.
     */
    public String refreshTokenCookieName() {
        return refreshTokenCookieName;
    }

    /**
     * Build the HttpOnly {@code refreshToken} Set-Cookie using the request for SameSite/Secure detection.
     * The cookie is scoped to Path=/ so it is sent to the server on every request,
     * but since only the {@code refreshToken} mutation reads it, it is safe.
     */
    public ResponseCookie buildRefreshTokenCookie(String token, HttpServletRequest request) {
        boolean crossSite = isCrossSite(request);
        String sameSite = crossSite ? "None" : normalizeSameSite(sameSiteWhenSameSite);
        boolean secure = crossSite || request.isSecure();

        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(refreshTokenCookieName, token)
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
     * Clear the HttpOnly {@code refreshToken} cookie.
     */
    public ResponseCookie clearRefreshTokenCookie(HttpServletRequest request) {
        boolean crossSite = isCrossSite(request);
        String sameSite = crossSite ? "None" : normalizeSameSite(sameSiteWhenSameSite);
        boolean secure = crossSite || request.isSecure();

        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(refreshTokenCookieName, "")
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

    /**
     * Build the HttpOnly {@code refreshToken} Set-Cookie using Origin/server URI for SameSite/Secure detection.
     * Used by {@link com.espacogeek.geek.config.GraphQlCookieInterceptor} which has access to
     * the {@code WebGraphQlRequest} headers and URI but not to the raw {@code HttpServletRequest}.
     */
    public ResponseCookie buildRefreshTokenCookie(String token, String originHeader, URI serverUri) {
        boolean crossSite = isCrossSite(originHeader, serverUri);
        String sameSite = crossSite ? "None" : normalizeSameSite(sameSiteWhenSameSite);
        boolean secure = crossSite || "https".equalsIgnoreCase(serverUri.getScheme());

        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(refreshTokenCookieName, token)
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
     * Clear the HttpOnly {@code refreshToken} cookie using Origin/server URI for SameSite/Secure detection.
     */
    public ResponseCookie clearRefreshTokenCookie(String originHeader, URI serverUri) {
        boolean crossSite = isCrossSite(originHeader, serverUri);
        String sameSite = crossSite ? "None" : normalizeSameSite(sameSiteWhenSameSite);
        boolean secure = crossSite || "https".equalsIgnoreCase(serverUri.getScheme());

        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(refreshTokenCookieName, "")
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
