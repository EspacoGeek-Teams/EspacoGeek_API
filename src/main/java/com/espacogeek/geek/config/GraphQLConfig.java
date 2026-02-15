package com.espacogeek.geek.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.DataFetcherExceptionResolver;

import com.espacogeek.geek.exception.resolver.GenericExceptionResolver;
import com.espacogeek.geek.metrics.GraphQLExecutionInstrumentation;
import org.springframework.graphql.execution.GraphQlSourceBuilderCustomizer;

@Configuration
public class GraphQLConfig {

    public DataFetcherExceptionResolver customGraphQLExceptionResolver() {
        return new GenericExceptionResolver();
    }

    public GraphQlSourceBuilderCustomizer sourceBuilderCustomizer(GraphQLExecutionInstrumentation instrumentation) {
        return builder -> builder.configureGraphQlSource(source ->
            source.introspectionEnabled(true)
        );
    }
}
