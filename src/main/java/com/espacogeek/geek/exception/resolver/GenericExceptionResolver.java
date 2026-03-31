package com.espacogeek.geek.exception.resolver;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import com.espacogeek.geek.exception.AccessDeniedException;
import com.espacogeek.geek.exception.EmailAlreadyExistsException;
import com.espacogeek.geek.exception.GenericException;
import com.espacogeek.geek.exception.InputValidationException;
import com.espacogeek.geek.exception.InvalidCredentialsException;
import com.espacogeek.geek.exception.MediaAlreadyExist;
import com.espacogeek.geek.exception.MediaAlreadyInLibraryException;
import com.espacogeek.geek.exception.NotFoundException;
import com.espacogeek.geek.exception.TokenExpiredException;
import org.springframework.dao.DataAccessException;

import java.util.Map;

/**
 * Centralized GraphQL exception resolver that maps domain exceptions to
 * standardized error codes injected into the GraphQL {@code extensions} block
 * under the key {@code errorCode}.
 *
 * <p>Error code categories:
 * <ul>
 *   <li><b>1xxx – Authentication errors</b>
 *     <ul>
 *       <li>1001 – Invalid credentials</li>
 *       <li>1002 – Token expired/invalid</li>
 *     </ul>
 *   </li>
 *   <li><b>2xxx – Business rule errors</b>
 *     <ul>
 *       <li>2001 – Email already registered</li>
 *       <li>2003 – Media already exists</li>
 *       <li>2004 – Input validation failed</li>
 *       <li>2005 – Media already in library</li>
 *     </ul>
 *   </li>
 *   <li><b>3xxx – Resource errors</b>
 *     <ul>
 *       <li>3403 – Access denied (e.g. private library)</li>
 *       <li>3404 – Resource not found (e.g. user or media)</li>
 *     </ul>
 *   </li>
 *   <li><b>5xxx – Internal errors</b>
 *     <ul>
 *       <li>5000 – Unexpected server error</li>
 *       <li>5001 – Database error</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p>Business exceptions never expose a stack trace to the client – only the
 * message and {@code errorCode} are returned. Any unmapped exception is
 * logged as a server error and returned to the client as code {@code 5000}.
 */
@Component
@Slf4j
public class GenericExceptionResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(@NonNull Throwable exception, @NonNull DataFetchingEnvironment env) {
        return switch (exception) {
            case InvalidCredentialsException e  -> buildError(env, "Invalid credentials", 1001);
            case TokenExpiredException e        -> buildError(env, "Token expired/invalid", 1002);
            case EmailAlreadyExistsException e  -> buildError(env, "Email already registered", 2001);
            case MediaAlreadyExist e            -> buildError(env, "Media already exists", 2003);
            case MediaAlreadyInLibraryException e -> buildError(env, "Media already in library", 2005);
            case InputValidationException e     -> buildError(env, e.getMessage() != null ? e.getMessage() : "Input validation failed", 2004);
            case AccessDeniedException e        -> buildError(env, e.getMessage(), 3403);
            case NotFoundException e            -> buildError(env, e.getMessage(), 3404);
            case ValidationException e          -> buildError(env, "Input validation failed", 2004);
            case DataAccessException e          -> {
                log.error("Database error during GraphQL execution: {}", exception.getMessage(), exception);
                yield buildError(env, "Database error", 5001);
            }
            case GenericException e             -> buildError(env, e.getMessage(), 5000);
            default                            -> {
                log.error("Unexpected error during GraphQL execution: {}", exception.getMessage(), exception);
                yield buildError(env, "Unexpected server error", 5000);
            }
        };
    }

    private GraphQLError buildError(DataFetchingEnvironment env, String message, int errorCode) {
        return GraphqlErrorBuilder.newError(env)
                .message(message)
                .extensions(Map.of("errorCode", errorCode))
                .build();
    }
}

