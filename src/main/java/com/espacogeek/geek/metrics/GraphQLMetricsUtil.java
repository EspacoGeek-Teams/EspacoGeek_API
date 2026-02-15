package com.espacogeek.geek.metrics;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WebFilter para capturar e registrar métricas de requisições GraphQL
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GraphQLMetricsUtil implements WebFilter {

    private final GraphQLExecutionInstrumentation metricsInstrumentation;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // Verifica se é uma requisição GraphQL
        if (!isGraphQLRequest(exchange)) {
            return chain.filter(exchange);
        }

        long startTime = System.currentTimeMillis();

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    long duration = System.currentTimeMillis() - startTime;
                    int statusCode = exchange.getResponse().getStatusCode() != null
                            ? exchange.getResponse().getStatusCode().value()
                            : 200;

                    boolean success = statusCode >= 200 && statusCode < 400;

                    // Tentar extrair informações da query para métricas mais detalhadas
                    String operationName = "unknown";
                    String operationType = "unknown";

                    try {
                        // Extrair query da requisição (se disponível)
                        String query = extractGraphQLQuery(exchange);
                        if (query != null) {
                            operationType = GraphQLMetricsInstrumentation.extractOperationType(query);
                            operationName = GraphQLMetricsInstrumentation.extractOperationName(query);
                        }
                    } catch (Exception e) {
                        log.debug("Could not extract GraphQL operation info", e);
                    }

                    // Registrar métrica
                    metricsInstrumentation.recordOperation(operationName, operationType, duration, success);

                    log.debug("GraphQL Request - Status: {}, Duration: {}ms, Op: {}, Type: {}",
                            statusCode, duration, operationName, operationType);
                });
    }

    private boolean isGraphQLRequest(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();

        return (path.equals("/") || path.equals("/graphql")) &&
               ("POST".equals(method) || "GET".equals(method));
    }

    private String extractGraphQLQuery(ServerWebExchange exchange) {
        try {
            // Para POST requests com JSON
            if ("POST".equals(exchange.getRequest().getMethod().name())) {
                return exchange.getAttribute("graphql_query");
            }
        } catch (Exception e) {
            log.debug("Could not extract GraphQL query", e);
        }
        return null;
    }
}



