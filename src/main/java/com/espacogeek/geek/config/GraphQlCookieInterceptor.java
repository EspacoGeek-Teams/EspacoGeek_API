package com.espacogeek.geek.config;

import java.net.URI;
import java.util.Map;

import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;

import reactor.core.publisher.Mono;

/**
 * Interceptor to set/clear HttpOnly auth cookie after login/logout GraphQL operations.
 */
@Configuration
public class GraphQlCookieInterceptor implements WebGraphQlInterceptor {

    private final JwtConfig jwtConfig;

    public GraphQlCookieInterceptor(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    @SuppressWarnings("null")
    @Override
    public @NonNull Mono<WebGraphQlResponse> intercept(@NonNull WebGraphQlRequest request, @NonNull Chain chain) {
        return chain.next(request).map(response -> {
            String operationName = request.getOperationName();
            if (!StringUtils.hasText(operationName)) {
                operationName = extractTopFieldName(request.getDocument());
            }

            if (operationName != null) {
                URI serverUri = request.getUri().toUri();
                String origin = request.getHeaders().getFirst(HttpHeaders.ORIGIN);

                Map<String, Object> data = response.getData();
                if ("login".equals(operationName.toLowerCase())) {
                    if (data != null) {
                        Object val = data.get("login");
                        if (val instanceof String token && !token.isBlank()) {
                            ResponseCookie cookie = jwtConfig.buildAuthCookie(token, origin, serverUri);
                            response.getResponseHeaders().add(HttpHeaders.SET_COOKIE, cookie.toString());
                        }
                    }
                } else if ("logout".equals(operationName.toLowerCase())) {
                    ResponseCookie clear = jwtConfig.clearAuthCookie(origin, serverUri);
                    response.getResponseHeaders().add(HttpHeaders.SET_COOKIE, clear.toString());
                }
            }
            return response;
        });
    }

    private String extractTopFieldName(String document) {
        if (document == null) return null;
        String s = document.stripLeading();
        int idx = s.indexOf('{');
        if (idx >= 0) {
            String after = s.substring(idx + 1).trim();
            int end = after.indexOf('(');
            int space = after.indexOf(' ');
            int brace = after.indexOf('}');
            int cut = Integer.MAX_VALUE;
            if (end >= 0) cut = Math.min(cut, end);
            if (space >= 0) cut = Math.min(cut, space);
            if (brace >= 0) cut = Math.min(cut, brace);
            if (cut != Integer.MAX_VALUE) {
                return after.substring(0, cut).trim();
            }
        }
        return null;
    }
}
