package com.espacogeek.geek.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${spring.mvc.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Value("${security.jwt.expiration-ms:604800000}")
    private long expirationMs;

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        String[] origins = parseOrigins();

        registry.addMapping("/**")
            .allowedOrigins(origins) // exact origins, not '*'
            .allowCredentials(true) // Access-Control-Allow-Credentials: true
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("Authorization", "Content-Type", "X-Requested-With", "Accept")
            .exposedHeaders("Authorization", "Content-Type")
            .maxAge(expirationMs / 1000); // seconds
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        String[] origins = parseOrigins();

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(origins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept"));
        config.setExposedHeaders(List.of("Authorization", "Content-Type"));
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
