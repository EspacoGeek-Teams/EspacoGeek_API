package com.espacogeek.geek.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CorsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import com.espacogeek.geek.services.impl.UserDetailsServiceImpl;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;

/**
 * Spring Security setup: fully stateless JWT authentication.
 * CSRF protection is disabled because all state-mutating requests are authenticated
 * via the {@code Authorization: Bearer <accessToken>} header. Browsers never
 * automatically add that header to cross-origin requests, so CSRF cannot occur.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${spring.mvc.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Value("${security.jwt.expiration-ms:604800000}")
    private long expirationMs;

    @Value("${app.monitoring.prometheus-token:}")
    private String prometheusToken;

    @PostConstruct
    public void logCorsConfig() {
        log.info("CORS Configuration loaded:");
        log.info("Allowed Origins: {}", allowedOrigins);
        String[] origins = parseOrigins();
        for (String origin : origins) {
            log.info("  - {}", origin);
        }
        log.info("JWT Expiration (ms): {}", expirationMs);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain configure(HttpSecurity http) throws Exception {
        var authenticationManagerBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.userDetailsService(userDetailsService).passwordEncoder(bCryptPasswordEncoder());
        var authenticationManager = authenticationManagerBuilder.build();

        return http
                .cors(this::corsSettings)
                // CSRF disabled: all API requests are authenticated via the short-lived
                // JWT access token in the Authorization: Bearer header (never sent
                // automatically by browsers), making CSRF attacks impossible.
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/", "/graphql", "/graphiql", "/graphiql/**", "/favicon.ico").permitAll();
                    auth.requestMatchers(request -> {
                        String header = request.getHeader("X-Prometheus-Token");
                        return request.getServletPath().startsWith("/actuator") &&
                               !prometheusToken.isBlank() &&
                               header != null &&
                               MessageDigest.isEqual(prometheusToken.getBytes(), header.getBytes());
                    }).permitAll();
                    auth.requestMatchers("/actuator/**").denyAll();
                    auth.requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll();
                    auth.anyRequest().authenticated();
                })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationManager(authenticationManager)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    private void corsSettings(CorsConfigurer<HttpSecurity> cors) {
        String[] origins = parseOrigins();

        cors.configurationSource(request -> {
            CorsConfiguration configuration = new CorsConfiguration();
            configuration.setAllowedOrigins(Arrays.asList(origins));
            configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
            configuration.setAllowedHeaders(List.of("*"));
            configuration.setExposedHeaders(List.of("Authorization", "Content-Type", "Set-Cookie"));
            configuration.setAllowCredentials(true);
            configuration.setMaxAge(expirationMs / 1000);
            return configuration;
        });
    }

    // !---------- Helpers

    private String[] parseOrigins() {
        return Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toArray(String[]::new);
    }
}
