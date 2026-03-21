package com.espacogeek.geek.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Serves the standalone Account Settings page at {@code /settings}.
 * <p>
 * The page is a self-contained HTML/CSS/JS file that communicates with the
 * GraphQL API to let authenticated users manage their profile (username, email,
 * password), trigger password resets, and — when they hold {@code ROLE_admin} —
 * access the admin panel.
 */
@Configuration
public class SettingsPageConfiguration {

    @Bean
    @Order(0)
    public RouterFunction<ServerResponse> settingsRouterFunction() {
        ClassPathResource settingsPage = new ClassPathResource("settings/index.html");
        return RouterFunctions.route()
                .GET("/settings", request -> ServerResponse.ok()
                        .contentType(org.springframework.http.MediaType.TEXT_HTML)
                        .body(settingsPage))
                .build();
    }
}
