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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interceptor to set/clear HttpOnly auth cookie after login/logout GraphQL operations.
 */
@Configuration
public class GraphQlCookieInterceptor implements WebGraphQlInterceptor {

    private static final Logger log = LoggerFactory.getLogger(GraphQlCookieInterceptor.class); // ! debug print

    private final JwtConfig jwtConfig;

    public GraphQlCookieInterceptor(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    @Override
    public @NonNull Mono<WebGraphQlResponse> intercept(@NonNull WebGraphQlRequest request, @NonNull Chain chain) {
        // ! debug print
        log.info("GraphQlCookieInterceptor.intercept - incoming operationName={} uri={} headersOrigin={}", request.getOperationName(), request.getUri(), request.getHeaders().getFirst(HttpHeaders.ORIGIN));
        return chain.next(request).map(response -> {
            String operationName = request.getOperationName();
            if (!StringUtils.hasText(operationName)) {
                operationName = extractTopFieldName(request.getDocument());
            }

            if (operationName != null) {
                URI serverUri = request.getUri().toUri();
                String origin = request.getHeaders().getFirst(HttpHeaders.ORIGIN);

                Map<String, Object> data = response.getData();
                if ("login".equals(operationName)) {
                    log.info("GraphQlCookieInterceptor handling 'login'"); // ! debug print
                    if (data != null) {
                        Object val = data.get("login");
                        if (val instanceof String token && !token.isBlank()) {
                            ResponseCookie cookie = jwtConfig.buildAuthCookie(token, origin, serverUri);
                            response.getResponseHeaders().add(HttpHeaders.SET_COOKIE, cookie.toString());
                            log.info("Added Set-Cookie header name={} httpOnly={} secure={} path={} maxAge={} domain={} origin={} serverUri={}", // ! debug print
                                    cookie.getName(), cookie.isHttpOnly(), cookie.isSecure(), cookie.getPath(),
                                    cookie.getMaxAge(), cookie.getDomain(), origin, serverUri);
                        } else {
                            log.warn("login data missing or token empty: data={}", data); // ! debug print
                        }
                    } else {
                        log.warn("GraphQL response data is null for 'login'"); // ! debug print
                    }
                } else if ("logout".equals(operationName)) {
                    log.info("GraphQlCookieInterceptor handling 'logout'"); // ! debug print
                    ResponseCookie clear = jwtConfig.clearAuthCookie(origin, serverUri);
                    response.getResponseHeaders().add(HttpHeaders.SET_COOKIE, clear.toString());
                    log.info("Added Clear-Cookie header name={} httpOnly={} secure={} path={} maxAge={} domain={} origin={} serverUri={}", // ! debug print
                            clear.getName(), clear.isHttpOnly(), clear.isSecure(), clear.getPath(), clear.getMaxAge(),
                            clear.getDomain(), origin, serverUri);
                } else {
                    log.debug("GraphQlCookieInterceptor bypass operationName={}", operationName); // ! debug print
                }
            } else {
                log.debug("GraphQlCookieInterceptor could not determine operation name"); // ! debug print
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
