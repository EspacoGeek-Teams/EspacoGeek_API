package com.espacogeek.geek.query.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import com.espacogeek.geek.services.EmailService;
import com.espacogeek.geek.services.EmailVerificationService;
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

/**
 * Integration test verifying that a successful {@code login} mutation:
 * <ul>
 *   <li>Returns an {@code accessToken} in the JSON payload.</li>
 *   <li>Sets an HttpOnly {@code refreshToken} cookie in the response.</li>
 *   <li>The cookie has the correct security attributes (HttpOnly, Path=/).</li>
 * </ul>
 */
@SpringBootTest(classes = LoginCookieHeaderTest.TestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@SuppressWarnings("null")
class LoginCookieHeaderTest {

    @SpringBootApplication
    @Import({UserController.class, JwtConfig.class, GraphQlCookieInterceptor.class})
    static class TestConfig {
        @Bean
        RuntimeWiringConfigurer dateScalarConfigurer() {
            return builder -> builder.scalar(Scalars.dateType());
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtConfig jwtConfig;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private EmailVerificationService emailVerificationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void login_ShouldReturnAccessTokenInBody_AndSetRefreshTokenCookie() throws Exception {
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
        when(jwtTokenService.saveToken(any(), any(UserModel.class), any())).thenReturn(null);

        String mutation = """
                mutation($email: String!, $password: String!) {
                    login(email: $email, password: $password) {
                        accessToken
                        user { id username email }
                    }
                }
                """;
        Map<String, Object> requestBody = Map.of(
                "query", mutation,
                "variables", Map.of("email", email, "password", password)
        );
        String json = objectMapper.writeValueAsString(requestBody);

        String origin = "http://client.example"; // cross-site origin to trigger SameSite=None; Secure

        // When
        MvcResult mvcResult = mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Origin", origin)
                        .content(json))
                .andExpect(request().asyncStarted())
                .andReturn();

        MvcResult result = mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andReturn();

        // Then: body contains a non-blank accessToken
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        String accessToken = objectMapper.readTree(body)
                .path("data").path("login").path("accessToken").asText();
        assertThat(accessToken).as("accessToken in JSON payload").isNotBlank();

        // Then: the refreshToken HttpOnly cookie must be present in Set-Cookie headers
        List<String> setCookies = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        assertThat(setCookies).as("Set-Cookie headers").isNotNull().isNotEmpty();

        String refreshTokenCookieName = jwtConfig.refreshTokenCookieName();
        String refreshCookieHeader = setCookies.stream()
                .filter(c -> c.startsWith(refreshTokenCookieName + "="))
                .findFirst()
                .orElse(null);
        assertThat(refreshCookieHeader).as("refreshToken cookie should be present").isNotNull();

        // Cookie must be HttpOnly and Path=/
        assertThat(refreshCookieHeader).contains("HttpOnly");
        assertThat(refreshCookieHeader).contains("Path=/");
        // Cross-site origin -> SameSite=None; Secure
        assertThat(refreshCookieHeader).contains("SameSite=None");
        assertThat(refreshCookieHeader).contains("Secure");
    }
}

