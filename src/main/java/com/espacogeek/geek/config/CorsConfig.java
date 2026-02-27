package com.espacogeek.geek.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

import jakarta.annotation.PostConstruct;

/**
 * CORS configuration using only the Spring Security CorsConfigurationSource bean.
 * <p>
 * Do NOT additionally override {@code WebMvcConfigurer#addCorsMappings} – having
 * both an MVC-level and a Security-level CORS config can cause the MVC CORS
 * interceptor to conflict with the Security CORS filter, leading to unexpected
 * 403 responses on browser requests while tool-based clients (Postman, curl)
 * continue to work.
 */
@Slf4j
@Configuration
public class CorsConfig {

    @Value("${spring.mvc.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @PostConstruct
    public void logCorsConfig() {
        log.info("CORS Configuration loaded:");
        log.info("Allowed Origins: {}", allowedOrigins);
        String[] origins = parseOrigins();
        for (String origin : origins) {
            log.info("  - {}", origin);
        }
    }

    @Value("${security.jwt.expiration-ms:604800000}")
    private long expirationMs;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        String[] origins = parseOrigins();

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(origins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Content-Type", "Set-Cookie"));
        config.setAllowCredentials(true);
        config.setMaxAge(expirationMs / 1000);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private String[] parseOrigins() {
        return Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toArray(String[]::new);
    }
}
