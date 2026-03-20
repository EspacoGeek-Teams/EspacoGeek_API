package com.espacogeek.geek.mutation.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.espacogeek.geek.config.GraphQlCookieInterceptor;
import com.espacogeek.geek.config.JwtConfig;
import com.espacogeek.geek.controllers.UserController;
import com.espacogeek.geek.models.UserModel;
import com.espacogeek.geek.services.EmailService;
import com.espacogeek.geek.services.EmailVerificationService;
import com.espacogeek.geek.services.JwtTokenService;
import com.espacogeek.geek.services.UserService;
import com.espacogeek.geek.types.Scalars;
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

/**
 * Integration test verifying that the {@code refreshToken} mutation:
 * <ul>
 *   <li>Reads the refresh token from the {@code refreshToken} HttpOnly cookie (via
 *       {@link GraphQlCookieInterceptor} context injection, NOT via {@code @CookieValue}).</li>
 *   <li>Returns a new {@code accessToken} in the JSON payload.</li>
 *   <li>Sets a rotated {@code refreshToken} cookie in the response.</li>
 *   <li>Returns an error when the cookie is absent.</li>
 * </ul>
 */
@SpringBootTest(classes = RefreshTokenMutationTest.TestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@SuppressWarnings("null")
class RefreshTokenMutationTest {

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

    private static final String MUTATION = """
            mutation {
                refreshToken {
                    accessToken
                    user { id username email }
                }
            }
            """;

    @Test
    void refreshToken_WithValidCookie_ShouldReturnNewAccessTokenAndSetCookie() throws Exception {
        // Given
        UserModel user = new UserModel();
        user.setId(1);
        user.setUsername("testuser");
        user.setEmail("user@example.com");

        String existingRefreshToken = jwtConfig.generateRefreshToken(user);

        when(jwtTokenService.isTokenValid(existingRefreshToken)).thenReturn(true);
        when(userService.findUserByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(jwtTokenService.saveToken(any(), any(UserModel.class), any())).thenReturn(null);

        String json = objectMapper.writeValueAsString(Map.of("query", MUTATION));

        // When
        MvcResult mvcResult = mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Cookie", jwtConfig.refreshTokenCookieName() + "=" + existingRefreshToken)
                        .content(json))
                .andExpect(request().asyncStarted())
                .andReturn();

        MvcResult result = mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andReturn();

        // Then: body contains a non-blank accessToken
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        String accessToken = objectMapper.readTree(body)
                .path("data").path("refreshToken").path("accessToken").asText();
        assertThat(accessToken).as("new accessToken in JSON payload").isNotBlank();

        // Then: a new refreshToken cookie is set in Set-Cookie headers
        List<String> setCookies = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        assertThat(setCookies).as("Set-Cookie headers").isNotNull().isNotEmpty();

        String refreshCookieHeader = setCookies.stream()
                .filter(c -> c.startsWith(jwtConfig.refreshTokenCookieName() + "="))
                .findFirst()
                .orElse(null);
        assertThat(refreshCookieHeader).as("rotated refreshToken cookie should be present").isNotNull();
        assertThat(refreshCookieHeader).contains("HttpOnly");
        assertThat(refreshCookieHeader).contains("Path=/");
    }

    @Test
    void refreshToken_WithoutCookie_ShouldReturnError() throws Exception {
        // Given
        String json = objectMapper.writeValueAsString(Map.of("query", MUTATION));

        // When — send request without any Cookie header
        MvcResult mvcResult = mockMvc.perform(post("/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn();

        // If the response was async, dispatch it; otherwise use the result directly.
        String body;
        if (mvcResult.getRequest().isAsyncStarted()) {
            MvcResult asyncResult = mockMvc.perform(asyncDispatch(mvcResult))
                    .andExpect(status().isOk())
                    .andReturn();
            body = asyncResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        } else {
            body = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        }

        // Then: errors array is non-empty (no cookie → TokenExpiredException)
        assertThat(objectMapper.readTree(body).path("errors").isEmpty()).isFalse();
    }
}
