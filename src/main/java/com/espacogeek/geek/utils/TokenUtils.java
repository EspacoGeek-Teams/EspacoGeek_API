package com.espacogeek.geek.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.espacogeek.geek.config.JwtConfig;
import com.espacogeek.geek.services.JwtTokenService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class TokenUtils {
    @Autowired
    private JwtConfig jwtConfig;

    @Autowired
    private JwtTokenService jwtTokenService;

    public String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (jwtConfig.cookieName().equals(c.getName())) {
                    String val = c.getValue();
                    if (val != null && !val.isBlank()) {
                        return val;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Resolve token from the current request obtained via RequestContextHolder.
     * If the servlet request is not available (GraphQL execution context), fall back
     * to reading the token from the SecurityContext authentication (credentials/details).
     */
    public String resolveToken() {
        String token = null;

        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            if (request != null) {
                String t = resolveToken(request);
                if (t != null && !t.isBlank()) token = t;
            }
        }

        // Fallback: get token from SecurityContext (set by JwtAuthenticationFilter)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            Object creds = auth.getCredentials();
            if (creds instanceof String s && !s.isBlank()) token = s;
            Object details = auth.getDetails();
            if (details instanceof String d && !d.isBlank()) token = d;
        }


        boolean isValid = jwtTokenService.isTokenValid(token); // Ensure token is valid

        if (!isValid) {
            return null;
        }

        return token;
    }
}
