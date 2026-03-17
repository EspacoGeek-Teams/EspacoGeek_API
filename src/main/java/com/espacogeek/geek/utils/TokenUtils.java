package com.espacogeek.geek.utils;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility for extracting the JWT access token from the current request.
 * Access tokens are read ONLY from the {@code Authorization: Bearer} header —
 * never from cookies — to eliminate CSRF attack vectors.
 */
@Component
public class TokenUtils {

    /**
     * Extract the access token from the {@code Authorization: Bearer} header of the given request.
     * Returns {@code null} if the header is absent or not in Bearer format.
     */
    public String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    /**
     * Extract the access token from the current request via {@link RequestContextHolder}.
     * Falls back to reading from the {@link SecurityContextHolder} when the servlet
     * request is not available (e.g., deep inside a GraphQL execution context).
     */
    public String resolveToken() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            if (request != null) {
                String t = resolveToken(request);
                if (t != null && !t.isBlank()) return t;
            }
        }

        // Fallback: get token from SecurityContext (set by JwtAuthenticationFilter)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            Object creds = auth.getCredentials();
            if (creds instanceof String s && !s.isBlank()) return s;
            Object details = auth.getDetails();
            if (details instanceof String d && !d.isBlank()) return d;
        }

        return null;
    }
}
