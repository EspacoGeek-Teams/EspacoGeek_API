package com.espacogeek.geek.query.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.espacogeek.geek.config.JwtConfig;
import com.espacogeek.geek.models.UserModel;
import com.espacogeek.geek.services.JwtTokenService;
import com.espacogeek.geek.services.UserService;
import com.espacogeek.geek.utils.TokenUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import at.favre.lib.crypto.bcrypt.BCrypt;

import com.espacogeek.geek.controllers.UserController;
import com.espacogeek.geek.config.GraphQlCookieInterceptor;
import com.espacogeek.geek.types.Scalars;

// Use a minimal Spring Boot context importing only the required beans
@SpringBootTest(classes = LoginCookieHeaderTest.TestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@SuppressWarnings("null")
class LoginCookieHeaderTest {

    // Minimal configuration for this test: import only required beans
    @SpringBootApplication
    @Import({UserController.class, JwtConfig.class, GraphQlCookieInterceptor.class})
    static class TestConfig {
        // Register custom GraphQL scalars used by the schema (e.g., Date)
        @Bean
        RuntimeWiringConfigurer dateScalarConfigurer() {
            return builder -> builder.scalar(Scalars.dateType());
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtConfig jwtConfig; // use real JwtConfig to build cookie

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    // Necessário para satisfazer a dependência do UserController
    @MockitoBean
    private TokenUtils tokenUtils;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void login_ShouldSetAuthCookie_MatchingBodyToken() throws Exception {
        // Given
        String email = "user@example.com";
        String password = "ValidPassword123!";
        String hashedPassword = new String(BCrypt.withDefaults().hash(12, password.toCharArray()), StandardCharsets.UTF_8);

        UserModel user = new UserModel();
        user.setId(1);
        user.setUsername("testuser");
        user.setEmail(email);
        user.setPassword(hashedPassword.getBytes(StandardCharsets.UTF_8));

        when(userService.findUserByEmail(email)).thenReturn(Optional.of(user));
        when(jwtTokenService.saveToken(anyString(), any(UserModel.class), anyString())).thenReturn(null);

        // Build GraphQL request with variables to avoid escaping issues
        String query = "query($email: String!, $password: String!) { login(email: $email, password: $password) }";
        Map<String, Object> requestBody = Map.of(
                "query", query,
                "variables", Map.of("email", email, "password", password)
        );
        String json = objectMapper.writeValueAsString(requestBody);

        String origin = "http://client.example"; // cross-site to trigger SameSite=None; Secure

        // When
        MvcResult mvcResult = mockMvc.perform(post("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Origin", origin)
                        .content(json))
                .andExpect(request().asyncStarted())
                .andReturn();

        MvcResult result = mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andReturn();

        // Then: body contains token
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        String tokenFromBody = objectMapper.readTree(body)
                .path("data").path("login").asText();
        assertThat(tokenFromBody).isNotBlank();

        // Then: Set-Cookie header exists and contains the same token
        List<String> setCookies = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        assertThat(setCookies).isNotNull();
        assertThat(setCookies).isNotEmpty();

        String cookieName = jwtConfig.cookieName();
        String authCookie = setCookies.stream()
                .filter(c -> c.startsWith(cookieName + "="))
                .findFirst()
                .orElse(null);
        assertThat(authCookie).as("Auth cookie should be present").isNotNull();

        // cookie format: NAME=VALUE; Path=/; HttpOnly; SameSite=...; Secure?
        String cookieValue = authCookie.substring((cookieName + "=").length(), authCookie.indexOf(';'));
        assertThat(cookieValue).isEqualTo(tokenFromBody);

        // Basic attributes
        assertThat(authCookie).contains("HttpOnly");
        assertThat(authCookie).contains("Path=/");
        // Cross-site -> SameSite=None; Secure
        assertThat(authCookie).contains("SameSite=None");
        assertThat(authCookie).contains("Secure");
    }
}
