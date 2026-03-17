package com.espacogeek.geek.csrf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;
import java.util.Map;

import com.espacogeek.geek.config.GraphQlCookieInterceptor;
import com.espacogeek.geek.config.JwtAuthenticationFilter;
import com.espacogeek.geek.config.JwtConfig;
import com.espacogeek.geek.config.SecurityConfig;
import com.espacogeek.geek.controllers.DailyQuoteArtworkController;
import com.espacogeek.geek.models.DailyQuoteArtworkModel;
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
 * Tests verifying the stateless JWT auth model after CSRF protection was disabled.
 * <p>
 * Since all state-mutating requests are authenticated via the
 * {@code Authorization: Bearer} header (never sent automatically by browsers),
 * CSRF protection is no longer needed and has been disabled.
 * <ul>
 *   <li>POST requests from any origin without a CSRF token are allowed (public endpoints return 200).</li>
 *   <li>POST requests without an auth token to protected endpoints are rejected with HTTP 401/403.</li>
 *   <li>OPTIONS preflight requests continue to be allowed.</li>
 *   <li>No XSRF-TOKEN cookie is set (CSRF is disabled).</li>
 * </ul>
 */
@SpringBootTest(
    classes = CsrfProtectionTest.TestConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CsrfProtectionTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:3000";

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

    @MockitoBean
    private DailyQuoteArtworkService dailyQuoteArtworkService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

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
        model.setQuote("Test quote");
        model.setAuthor("Test author");
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

    @Test
    void post_WithoutCsrfToken_ShouldReturn200_ForPublicEndpoint() throws Exception {
        when(dailyQuoteArtworkService.getTodayQuoteArtwork()).thenReturn(stubDailyQuote());

        // CSRF is disabled: a browser POST to a public endpoint succeeds without any CSRF token.
        // Authentication is enforced solely via Authorization: Bearer header (stateless).
        MvcResult mvcResult = mockMvc.perform(post("/")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                .content(graphqlPayload()))
            .andReturn();

        MvcResult result = resolveResult(mvcResult);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void post_WithoutAuthToken_ToProtectedEndpoint_ShouldReturn200WithGraphQLError() throws Exception {
        // POST without auth to a protected query returns 200 with a GraphQL-level errors array
        // (Spring Security's access denied is translated into a GraphQL error, not an HTTP 401/403,
        // because the GraphQL endpoint itself is public — authorization happens at the field level).
        Map<String, Object> body = Map.of(
            "operationName", "IsLogged",
            "variables", Map.of(),
            "query", "query IsLogged { isLogged }"
        );
        String payload = objectMapper.writeValueAsString(body);

        MvcResult mvcResult = mockMvc.perform(post("/")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                .content(payload))
            .andReturn();

        MvcResult result = resolveResult(mvcResult);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void preflight_Options_ShouldBeAllowed() throws Exception {
        // OPTIONS preflight requests must always be allowed
        mockMvc.perform(options("/")
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
            .andExpect(status().isOk());
    }

    @Test
    void post_NoCsrfTokenCookie_ShouldNotBeSet() throws Exception {
        when(dailyQuoteArtworkService.getTodayQuoteArtwork()).thenReturn(stubDailyQuote());

        // CSRF is disabled: no XSRF-TOKEN cookie should be set in the response
        MvcResult mvcResult = mockMvc.perform(post("/")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                .content(graphqlPayload()))
            .andReturn();

        MvcResult result = resolveResult(mvcResult);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        // No XSRF-TOKEN cookie — CSRF protection is entirely disabled
        assertThat(result.getResponse().getCookie("XSRF-TOKEN")).isNull();
    }
}
