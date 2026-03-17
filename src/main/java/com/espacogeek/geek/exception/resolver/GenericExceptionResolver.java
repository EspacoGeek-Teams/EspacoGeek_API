package com.espacogeek.geek.exception.resolver;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import com.espacogeek.geek.exception.EmailAlreadyExistsException;
import com.espacogeek.geek.exception.GenericException;
import com.espacogeek.geek.exception.InputValidationException;
import com.espacogeek.geek.exception.InvalidCredentialsException;
import com.espacogeek.geek.exception.MediaAlreadyExist;
import com.espacogeek.geek.exception.TokenExpiredException;
import org.springframework.dao.DataAccessException;

import java.util.Map;

/**
 * Centralized GraphQL exception resolver that maps domain exceptions to
 * standardized error codes injected into the GraphQL {@code extensions} block
 * under the key {@code customNumber}.
 *
 * <p>Error code dictionary:
 * <ul>
 *   <li>1001 – Credenciais inválidas</li>
 *   <li>1002 – Token expirado/inválido</li>
 *   <li>2001 – E-mail já cadastrado</li>
 *   <li>2003 – Mídia já existe</li>
 *   <li>2004 – Validação de input falhou</li>
 *   <li>5000 – Erro inesperado do servidor</li>
 *   <li>5001 – Erro de banco de dados</li>
 * </ul>
 *
 * <p>Business exceptions never expose a stack trace to the client – only the
 * message and {@code customNumber} are returned. Any unmapped exception is
 * logged as a server error and returned to the client as code {@code 5000}.
 */
@Component
@Slf4j
public class GenericExceptionResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(@NonNull Throwable exception, @NonNull DataFetchingEnvironment env) {
        if (exception instanceof InvalidCredentialsException) {
            return buildError(env, "Credenciais inválidas", 1001);
        } else if (exception instanceof TokenExpiredException) {
            return buildError(env, "Token expirado/inválido", 1002);
        } else if (exception instanceof EmailAlreadyExistsException) {
            return buildError(env, "E-mail já cadastrado", 2001);
        } else if (exception instanceof MediaAlreadyExist) {
            return buildError(env, "Mídia já existe", 2003);
        } else if (exception instanceof InputValidationException || exception instanceof ValidationException) {
            return buildError(env, "Validação de input falhou", 2004);
        } else if (exception instanceof DataAccessException) {
            log.error("Database error during GraphQL execution: {}", exception.getMessage(), exception);
            return buildError(env, "Erro de banco de dados", 5001);
        } else if (exception instanceof GenericException) {
            return buildError(env, exception.getMessage(), 5000);
        } else {
            log.error("Unexpected error during GraphQL execution: {}", exception.getMessage(), exception);
            return buildError(env, "Erro inesperado do servidor", 5000);
        }
    }

    private GraphQLError buildError(DataFetchingEnvironment env, String message, int customNumber) {
        return GraphqlErrorBuilder.newError(env)
                .message(message)
                .extensions(Map.of("customNumber", customNumber))
                .build();
    }
}
