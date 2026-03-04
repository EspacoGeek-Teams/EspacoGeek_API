package com.espacogeek.geek.cors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

import com.espacogeek.geek.config.GraphQlCookieInterceptor;
import com.espacogeek.geek.config.JwtAuthenticationFilter;
import com.espacogeek.geek.config.JwtConfig;
import com.espacogeek.geek.config.SecurityConfig;
import com.espacogeek.geek.controllers.DailyQuoteArtworkController;
import com.espacogeek.geek.controllers.MediaController;
import com.espacogeek.geek.models.DailyQuoteArtworkModel;
import com.espacogeek.geek.services.DailyQuoteArtworkService;
import com.espacogeek.geek.services.MediaService;
import com.espacogeek.geek.services.impl.UserDetailsServiceImpl;
import com.espacogeek.geek.types.MediaPage;
import com.espacogeek.geek.types.MediaSimplefied;
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
 * E2E tests simulating browser CORS requests to the GraphQL API.
 * <p>
 * These tests verify that the CORS configuration (via Spring Security's CorsFilter)
 * correctly handles cross-origin requests from allowed origins while rejecting
 * requests from disallowed origins — reproducing the exact scenario where browsers
 * receive a 403 but tools like Postman work.
 */
@SpringBootTest(
    classes = BrowserCorsRequestTest.TestConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BrowserCorsRequestTest {

    // Default allowed origin in test (from application.properties default)
    private static final String ALLOWED_ORIGIN = "http://localhost:3000";
    private static final String DISALLOWED_ORIGIN = "https://evil.example.com";

    @SpringBootApplication
    @Import({
        DailyQuoteArtworkController.class,
        MediaController.class,
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
    private MediaService mediaService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @MockitoBean
    private TokenUtils tokenUtils;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Resolves the result of a MockMvc call, handling both sync and async GraphQL responses.
     */
    private MvcResult resolveResult(MvcResult mvcResult) throws Exception {
        if (mvcResult.getRequest().isAsyncStarted()) {
            return mockMvc.perform(asyncDispatch(mvcResult)).andReturn();
        }
        return mvcResult;
    }

    private DailyQuoteArtworkModel stubDailyQuote() {
        DailyQuoteArtworkModel model = new DailyQuoteArtworkModel();
        model.setId(1);
        model.setQuote("The only way to do great work is to love what you do.");
        model.setAuthor("Steve Jobs");
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

    private MediaPage stubTvSerieMediaPage() {
        MediaSimplefied item = new MediaSimplefied();
        item.setId(1);
        item.setName("Stranger Things");
        item.setCover("https://example.com/stranger-things.jpg");

        MediaPage page = new MediaPage();
        page.setContent(java.util.List.of(item));
        page.setTotalElements(1);
        page.setTotalPages(1);
        return page;
    }

    private String tvSeriePayload() throws Exception {
        Map<String, Object> body = Map.of(
            "operationName", "MediaPage",
            "variables", Map.of("name", "stranger"),
            "query", """
                query MediaPage($name: String) {
                    tvserie(name: $name) {
                        content {
                            id
                            name
                            cover
                        }
                        totalElements
                        totalPages
                    }
                }"""
        );
        return objectMapper.writeValueAsString(body);
    }

    // ---- CORS preflight tests ----

    @Test
    void corsPreflight_AllowedOrigin_ShouldReturnCorsHeaders() throws Exception {
        mockMvc.perform(options("/")
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    void corsPreflight_DisallowedOrigin_ShouldNotReturnAllowOriginHeader() throws Exception {
        MvcResult result = mockMvc.perform(options("/")
                .header(HttpHeaders.ORIGIN, DISALLOWED_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
            .andReturn();

        // CORS should reject: no Access-Control-Allow-Origin header for the disallowed origin
        String allowOrigin = result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
        assertThat(allowOrigin).isNull();
    }

    // ---- Browser POST tests (simulating real browser GraphQL request) ----

    @Test
    void browserPost_AllowedOrigin_ShouldReturn200WithCorsHeaders() throws Exception {
        when(dailyQuoteArtworkService.getTodayQuoteArtwork()).thenReturn(stubDailyQuote());

        // Simulate a browser POST with full browser headers (Origin, Sec-Fetch-*, etc.)
        MvcResult mvcResult = mockMvc.perform(post("/")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                .header(HttpHeaders.REFERER, ALLOWED_ORIGIN + "/")
                .header(HttpHeaders.ACCEPT, "*/*")
                .header("Sec-Fetch-Dest", "empty")
                .header("Sec-Fetch-Mode", "cors")
                .header("Sec-Fetch-Site", "same-site")
                .content(graphqlPayload()))
            .andReturn();

        MvcResult result = resolveResult(mvcResult);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
            .isEqualTo(ALLOWED_ORIGIN);
        assertThat(result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS))
            .isEqualTo("true");

        // Verify GraphQL response contains the expected data
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        var json = objectMapper.readTree(body);
        assertThat(json.path("data").path("dailyQuoteArtwork").path("quote").asText())
            .isEqualTo("The only way to do great work is to love what you do.");
        assertThat(json.path("data").path("dailyQuoteArtwork").path("author").asText())
            .isEqualTo("Steve Jobs");
    }

    @Test
    void browserPost_DisallowedOrigin_ShouldReturn403() throws Exception {
        when(dailyQuoteArtworkService.getTodayQuoteArtwork()).thenReturn(stubDailyQuote());

        // A browser request from a disallowed origin should be rejected
        mockMvc.perform(post("/")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ORIGIN, DISALLOWED_ORIGIN)
                .header("Sec-Fetch-Dest", "empty")
                .header("Sec-Fetch-Mode", "cors")
                .header("Sec-Fetch-Site", "cross-site")
                .content(graphqlPayload()))
            .andExpect(status().isForbidden());
    }

    @Test
    void browserPost_NoOriginHeader_ShouldReturn200() throws Exception {
        when(dailyQuoteArtworkService.getTodayQuoteArtwork()).thenReturn(stubDailyQuote());

        // A request without Origin header (like Postman/curl) should work
        MvcResult mvcResult = mockMvc.perform(post("/")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(graphqlPayload()))
            .andReturn();

        MvcResult result = resolveResult(mvcResult);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        var json = objectMapper.readTree(body);
        assertThat(json.path("data").path("dailyQuoteArtwork").path("quote").asText())
            .isEqualTo("The only way to do great work is to love what you do.");
    }

    @Test
    void browserPost_AllowedOrigin_ResponseShouldExposeCorrectHeaders() throws Exception {
        when(dailyQuoteArtworkService.getTodayQuoteArtwork()).thenReturn(stubDailyQuote());

        MvcResult mvcResult = mockMvc.perform(post("/")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                .content(graphqlPayload()))
            .andReturn();

        MvcResult result = resolveResult(mvcResult);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        // Verify exposed headers include the configured values
        String exposeHeaders = result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS);
        assertThat(exposeHeaders).isNotNull();
        assertThat(exposeHeaders).contains("Authorization");
        assertThat(exposeHeaders).contains("Content-Type");
        assertThat(exposeHeaders).contains("Set-Cookie");
    }

    // ---- tvserie MediaPage query tests ----

    @Test
    void tvSerieMediaPage_AllowedOrigin_ShouldReturn200WithCorsHeaders() throws Exception {
        when(mediaService.findSerieByIdOrName(
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq("stranger"),
                org.mockito.ArgumentMatchers.any()))
            .thenReturn(stubTvSerieMediaPage());

        // Reproduce the exact browser request from the issue report
        MvcResult mvcResult = mockMvc.perform(post("/")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                .header(HttpHeaders.REFERER, ALLOWED_ORIGIN + "/")
                .header(HttpHeaders.ACCEPT, "*/*")
                .header("Sec-Fetch-Dest", "empty")
                .header("Sec-Fetch-Mode", "cors")
                .header("Sec-Fetch-Site", "same-site")
                .content(tvSeriePayload()))
            .andReturn();

        MvcResult result = resolveResult(mvcResult);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
            .isEqualTo(ALLOWED_ORIGIN);
        assertThat(result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS))
            .isEqualTo("true");

        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        var json = objectMapper.readTree(body);
        assertThat(json.path("data").path("tvserie").path("content").get(0).path("name").asText())
            .isEqualTo("Stranger Things");
        assertThat(json.path("data").path("tvserie").path("totalElements").asLong())
            .isEqualTo(1);
    }

    @Test
    void tvSerieMediaPage_NoOriginHeader_ShouldReturn200() throws Exception {
        when(mediaService.findSerieByIdOrName(
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq("stranger"),
                org.mockito.ArgumentMatchers.any()))
            .thenReturn(stubTvSerieMediaPage());

        // A request without Origin header (like Postman/curl) should work — not return 403
        MvcResult mvcResult = mockMvc.perform(post("/")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(tvSeriePayload()))
            .andReturn();

        MvcResult result = resolveResult(mvcResult);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        var json = objectMapper.readTree(body);
        assertThat(json.path("data").path("tvserie").path("content").get(0).path("name").asText())
            .isEqualTo("Stranger Things");
    }

    @Test
    void tvSerieMediaPage_DisallowedOrigin_ShouldReturn403() throws Exception {
        // A browser request from a disallowed origin should be rejected even for tvserie
        mockMvc.perform(post("/")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ORIGIN, DISALLOWED_ORIGIN)
                .header("Sec-Fetch-Dest", "empty")
                .header("Sec-Fetch-Mode", "cors")
                .header("Sec-Fetch-Site", "cross-site")
                .content(tvSeriePayload()))
            .andExpect(status().isForbidden());
    }
}
