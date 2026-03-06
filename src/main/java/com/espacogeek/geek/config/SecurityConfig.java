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
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import com.espacogeek.geek.services.impl.UserDetailsServiceImpl;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security setup with JWT filter and method security.
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

    @Value("${security.csrf.cookie-domain:}")
    private String csrfCookieDomain;

    @Value("${security.csrf.cookie-same-site:}")
    private String csrfCookieSameSite;

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

        var csrfRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        if (!csrfCookieDomain.isBlank()) {
            csrfRepo.setCookieDomain(csrfCookieDomain);
        }
        if (!csrfCookieSameSite.isBlank()) {
            csrfRepo.setCookieCustomizer(builder -> builder.sameSite(csrfCookieSameSite));
        }

        return http
                .cors(this::corsSettings)
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepo)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/", "/graphiql", "/graphiql/**", "/favicon.ico").permitAll();
                    auth.requestMatchers("/actuator/**").permitAll();
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
