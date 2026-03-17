package com.espacogeek.geek.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

import com.espacogeek.geek.config.GraphQlCookieInterceptor;
import com.espacogeek.geek.config.JwtAuthenticationFilter;
import com.espacogeek.geek.config.JwtConfig;
import com.espacogeek.geek.config.SecurityConfig;
import com.espacogeek.geek.controllers.DailyQuoteArtworkController;
import com.espacogeek.geek.models.DailyQuoteArtworkModel;
import com.espacogeek.geek.models.UserModel;
import com.espacogeek.geek.services.DailyQuoteArtworkService;
import com.espacogeek.geek.services.impl.UserDetailsServiceImpl;
import com.espacogeek.geek.types.Scalars;
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

/**
 * Integration tests verifying the stateless JWT authentication architecture:
 * <ul>
 *   <li>API clients using {@code Authorization: Bearer} are authenticated and can access protected resources.</li>
 *   <li>CSRF is fully disabled — any POST (with or without a Bearer header) to public endpoints returns 200.</li>
 *   <li>Protected resources require a valid JWT access token in the Bearer header.</li>
 * </ul>
 * Both code paths share the same {@link SecurityConfig} and
 * {@link JwtAuthenticationFilter}, so the security context is unified for all GraphQL operations.
 */
@SpringBootTest(
    classes = BearerTokenAuthTest.TestConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BearerTokenAuthTest {

    @SpringBootApplication
    @Import({
        DailyQuoteArtworkController.class,
        SecurityConfig.class,
        JwtConfig.class,
        JwtAuthenticationFilter.class,
        GraphQlCookieInterceptor.class
    })
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
    private DailyQuoteArtworkService dailyQuoteArtworkService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    // TokenUtils has an @Autowired JwtTokenService dependency, so it requires a bean
    // even in a lightweight test context. Mocking it avoids bringing in the full
    // database-backed JWT token service while still allowing the security filter chain
    // (CSRF + authentication) to wire up correctly around it.
    @MockitoBean
    private TokenUtils tokenUtils;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MvcResult resolveResult(MvcResult mvcResult) throws Exception {
        if (mvcResult.getRequest().isAsyncStarted()) {
            return mockMvc.perform(asyncDispatch(mvcResult)).andReturn();
        }
        return mvcResult;
    }

    private DailyQuoteArtworkModel stubDailyQuote() {
        DailyQuoteArtworkModel model = new DailyQuoteArtworkModel();
        model.setId(1);
        model.setQuote("Bearer auth test quote");
        model.setAuthor("Test Author");
        model.setUrlArtwork("https://example.com/artwork.jpg");
        model.setDate(new Date());
        model.setCreatedAt(new Date());
        return model;
    }

    private String graphqlPayload() throws Exception {
        Map<String, Object> body = Map.of(
            "operationName", "DailyQuoteArtwork",
            "variables", Map.of(),
            "query", """
                query DailyQuoteArtwork {
                    dailyQuoteArtwork {
                        quote
                        author
                        urlArtwork
                    }
                }"""
        );
        return objectMapper.writeValueAsString(body);
    }

    /**
     * Mobile / API client sends a JWT as a Bearer token.
     * No CSRF token is included in the request.
     * Expected: 200 — the CSRF filter exempts requests that carry an Authorization: Bearer header.
     */
    @Test
    void bearerToken_WithoutCsrf_ShouldReturn200() throws Exception {
        when(dailyQuoteArtworkService.getTodayQuoteArtwork()).thenReturn(stubDailyQuote());

        UserModel testUser = new UserModel();
        testUser.setId(42);
        testUser.setEmail("mobile@example.com");
        testUser.setUsername("mobileuser");
        String token = jwtConfig.generateToken(testUser);

        MvcResult mvcResult = mockMvc.perform(post("/")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .content(graphqlPayload()))
            .andReturn();

        MvcResult result = resolveResult(mvcResult);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        var json = objectMapper.readTree(body);
        assertThat(json.path("data").path("dailyQuoteArtwork").path("quote").asText())
            .isEqualTo("Bearer auth test quote");
    }

    /**
     * Mobile / API client sends a cross-origin request with a Bearer token.
     * The allowed origin is included (e.g., a web-based mobile app or hybrid client).
     * No CSRF token is included.
     * Expected: 200 — CSRF is not required when Bearer is present, regardless of Origin.
     */
    @Test
    void bearerToken_AllowedOrigin_WithoutCsrf_ShouldReturn200() throws Exception {
        when(dailyQuoteArtworkService.getTodayQuoteArtwork()).thenReturn(stubDailyQuote());

        UserModel testUser = new UserModel();
        testUser.setId(43);
        testUser.setEmail("flutter@example.com");
        testUser.setUsername("flutteruser");
        String token = jwtConfig.generateToken(testUser);

        MvcResult mvcResult = mockMvc.perform(post("/")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .content(graphqlPayload()))
            .andReturn();

        MvcResult result = resolveResult(mvcResult);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
    }

    /**
     * Any POST from any origin without a Bearer token now succeeds for public endpoints
     * because CSRF is disabled. Authentication for protected endpoints is enforced
     * at the GraphQL field level via @PreAuthorize, returning a GraphQL error — not HTTP 403.
     */
    @Test
    void noBearerToken_WithoutCsrf_ShouldReturn200_ForPublicEndpoint() throws Exception {
        when(dailyQuoteArtworkService.getTodayQuoteArtwork()).thenReturn(stubDailyQuote());

        MvcResult mvcResult = mockMvc.perform(post("/")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .content(graphqlPayload()))
            .andReturn();

        MvcResult result = resolveResult(mvcResult);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
    }
}
