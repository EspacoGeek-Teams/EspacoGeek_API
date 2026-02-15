package com.espacogeek.geek.metrics;

import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Componente de métricas GraphQL para Prometheus
 * As métricas são capturadas via Controller Advice ou interceptadores HTTP
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GraphQLExecutionInstrumentation {

    private final MeterRegistry meterRegistry;

    /**
     * Registra métrica de operação GraphQL executada
     */
    public void recordOperation(String operationName, String operationType, long durationMs, boolean success) {
        try {
            // Garantir que operationName não seja nulo
            String name = operationName != null && !operationName.isEmpty() ? operationName : "anonymous";
            String type = operationType != null ? operationType.toLowerCase() : "unknown";

            // Métrica: Total de operações
            meterRegistry.counter(
                "graphql.operations.total",
                "operation", name,
                "type", type,
                "status", success ? "success" : "error"
            ).increment();

            // Métrica: Duração
            meterRegistry.timer(
                "graphql.operations.duration",
                "operation", name,
                "type", type
            ).record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);

            log.debug("GraphQL Metrics recorded - Op: {}, Type: {}, Duration: {}ms, Success: {}",
                    name, type, durationMs, success);
        } catch (Exception e) {
            log.error("Error recording GraphQL metrics", e);
        }
    }
}





