package com.espacogeek.geek.csrf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
 * Tests verifying that CSRF protection is enforced for cookie-authenticated browser requests.
 * <p>
 * Validates that:
 * <ul>
 *   <li>POST requests without a valid CSRF token are rejected with HTTP 403.</li>
 *   <li>POST requests including a valid CSRF token are accepted.</li>
 *   <li>The server sets the XSRF-TOKEN cookie so the frontend can obtain a CSRF token.</li>
 *   <li>CSRF is not enforced for safe methods (OPTIONS preflight).</li>
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
    void post_WithoutCsrfToken_ShouldReturn403() throws Exception {
        when(dailyQuoteArtworkService.getTodayQuoteArtwork()).thenReturn(stubDailyQuote());

        // A browser POST from an allowed origin without a CSRF token must be rejected
        mockMvc.perform(post("/")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                .content(graphqlPayload()))
            .andExpect(status().isForbidden());
    }

    @Test
    void post_WithValidCsrfToken_ShouldReturn200() throws Exception {
        when(dailyQuoteArtworkService.getTodayQuoteArtwork()).thenReturn(stubDailyQuote());

        // A browser POST from an allowed origin WITH a valid CSRF token must succeed
        MvcResult mvcResult = mockMvc.perform(post("/")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                .content(graphqlPayload()))
            .andReturn();

        MvcResult result = resolveResult(mvcResult);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void preflight_Options_ShouldNotRequireCsrfToken() throws Exception {
        // OPTIONS preflight requests must be allowed without a CSRF token
        mockMvc.perform(options("/")
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
            .andExpect(status().isOk());
    }

    @Test
    void firstRequest_ShouldSetXsrfTokenCookie() throws Exception {
        when(dailyQuoteArtworkService.getTodayQuoteArtwork()).thenReturn(stubDailyQuote());

        // A POST request with no existing XSRF-TOKEN cookie triggers CSRF token generation.
        // The server must set the XSRF-TOKEN cookie in the response (even on 403) so the
        // frontend can read the token and include it as the X-XSRF-TOKEN header in the retry.
        MvcResult result = mockMvc.perform(post("/")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                .content(graphqlPayload()))
            .andExpect(status().isForbidden())
            .andReturn();

        // XSRF-TOKEN cookie must be present and readable by JavaScript (not HttpOnly)
        assertThat(result.getResponse().getCookie("XSRF-TOKEN")).isNotNull();
        assertThat(result.getResponse().getCookie("XSRF-TOKEN").isHttpOnly()).isFalse();
    }

    @Test
    void post_SpaCsrfCookieFlow_ShouldReturn200() throws Exception {
        when(dailyQuoteArtworkService.getTodayQuoteArtwork()).thenReturn(stubDailyQuote());

        // Step 1: SPA makes first POST without any CSRF token.
        // Server rejects with 403 and sets the XSRF-TOKEN cookie so the SPA can read it.
        MvcResult firstResult = mockMvc.perform(post("/")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                .content(graphqlPayload()))
            .andExpect(status().isForbidden())
            .andReturn();

        // SPA reads the raw XSRF-TOKEN cookie value from the 403 response
        jakarta.servlet.http.Cookie xsrfCookie = firstResult.getResponse().getCookie("XSRF-TOKEN");
        assertThat(xsrfCookie).isNotNull();
        String csrfTokenValue = xsrfCookie.getValue();
        assertThat(csrfTokenValue).isNotBlank();

        // Step 2: SPA retries the POST, sending the raw cookie value as the X-XSRF-TOKEN header.
        // The browser also automatically includes the XSRF-TOKEN cookie in the request.
        // With CsrfTokenRequestAttributeHandler (plain), the raw value is compared directly
        // to the stored cookie — no XOR decoding — so the request is accepted.
        MvcResult mvcResult = mockMvc.perform(post("/")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                .header("X-XSRF-TOKEN", csrfTokenValue)
                .cookie(new jakarta.servlet.http.Cookie("XSRF-TOKEN", csrfTokenValue))
                .content(graphqlPayload()))
            .andReturn();

        MvcResult result = resolveResult(mvcResult);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
    }
}
