package com.espacogeek.geek.metrics;

import lombok.extern.slf4j.Slf4j;

/**
 * Utilitários para instrumentação GraphQL
 */
@Slf4j
public class GraphQLMetricsInstrumentation {

    /**
     * Extrai o nome da operação do documento GraphQL
     */
    public static String extractOperationName(String query) {
        try {
            if (query == null || query.trim().isEmpty()) {
                return "anonymous";
            }

            // Tenta extrair o nome da operação com regex simples
            // Formatos suportados: query NomeOperacao, mutation NomeOperacao, subscription NomeOperacao
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "(?:query|mutation|subscription)\\s+([a-zA-Z_][a-zA-Z0-9_]*)");
            java.util.regex.Matcher matcher = pattern.matcher(query);

            if (matcher.find()) {
                return matcher.group(1);
            }

            // Se não encontrou, tenta extrair a primeira palavra-chave GraphQL
            pattern = java.util.regex.Pattern.compile("(\\w+)\\s*(?:\\(|\\{)");
            matcher = pattern.matcher(query);
            if (matcher.find()) {
                return matcher.group(1);
            }

            return "anonymous";
        } catch (Exception e) {
            log.debug("Could not extract operation name", e);
            return "anonymous";
        }
    }

    /**
     * Determina o tipo de operação (query, mutation, subscription)
     */
    public static String extractOperationType(String query) {
        if (query == null) {
            return "unknown";
        }

        String trimmedQuery = query.trim().toLowerCase();
        if (trimmedQuery.startsWith("query")) {
            return "query";
        } else if (trimmedQuery.startsWith("mutation")) {
            return "mutation";
        } else if (trimmedQuery.startsWith("subscription")) {
            return "subscription";
        }
        return "unknown";
    }
}


