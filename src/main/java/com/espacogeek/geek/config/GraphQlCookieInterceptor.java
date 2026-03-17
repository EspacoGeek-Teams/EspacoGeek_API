package com.espacogeek.geek.config;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.NonNull;

import reactor.core.publisher.Mono;

/**
 * Interceptor that manages {@code refreshToken} HttpOnly cookie operations for login,
 * refreshToken, and logout GraphQL mutations.
 *
 * <p>Before execution begins, this interceptor injects two shared containers into the
 * {@link graphql.GraphQLContext}:</p>
 * <ul>
 *   <li>{@code "pendingRefreshTokens"} – controller methods add a refresh token here when
 *       one should be set as an HttpOnly cookie on the response.</li>
 *   <li>{@code "clearRefreshCookieHolder"} – a {@code boolean[1]} array; controller methods
 *       set index {@code [0]} to {@code true} to signal that the refresh token cookie should
 *       be cleared (e.g., on logout).</li>
 * </ul>
 * <p>After execution completes, this interceptor reads the containers and writes the
 * appropriate {@code Set-Cookie} header to the HTTP response via
 * {@link WebGraphQlResponse#getResponseHeaders()}.</p>
 */
@Configuration
public class GraphQlCookieInterceptor implements WebGraphQlInterceptor {

    private final JwtConfig jwtConfig;

    public GraphQlCookieInterceptor(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    @Override
    public @NonNull Mono<WebGraphQlResponse> intercept(@NonNull WebGraphQlRequest request, @NonNull Chain chain) {
        // Shared containers — populated by controller methods via GraphQLContext during execution
        List<String> pendingRefreshTokens = new ArrayList<>();
        boolean[] clearRefreshCookieHolder = {false};

        request.configureExecutionInput((input, builder) ->
                builder.graphQLContext(ctx -> {
                    ctx.put("pendingRefreshTokens", pendingRefreshTokens);
                    ctx.put("clearRefreshCookieHolder", clearRefreshCookieHolder);
                }).build());

        return chain.next(request).map(response -> {
            String origin = request.getHeaders().getFirst(HttpHeaders.ORIGIN);
            URI serverUri = request.getUri().toUri();

            if (!pendingRefreshTokens.isEmpty()) {
                ResponseCookie cookie = jwtConfig.buildRefreshTokenCookie(
                        pendingRefreshTokens.get(0), origin, serverUri);
                response.getResponseHeaders().add(HttpHeaders.SET_COOKIE, cookie.toString());
            }

            if (clearRefreshCookieHolder[0]) {
                ResponseCookie clear = jwtConfig.clearRefreshTokenCookie(origin, serverUri);
                response.getResponseHeaders().add(HttpHeaders.SET_COOKIE, clear.toString());
            }

            return response;
        });
    }
}
