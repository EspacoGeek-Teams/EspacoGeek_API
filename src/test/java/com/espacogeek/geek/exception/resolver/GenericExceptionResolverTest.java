package com.espacogeek.geek.exception.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import graphql.GraphQLError;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ResultPath;
import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.espacogeek.geek.exception.EmailAlreadyExistsException;
import com.espacogeek.geek.exception.GenericException;
import com.espacogeek.geek.exception.InputValidationException;
import com.espacogeek.geek.exception.InvalidCredentialsException;
import com.espacogeek.geek.exception.MediaAlreadyExist;
import com.espacogeek.geek.exception.TokenExpiredException;

import java.util.List;

/**
 * Unit tests for {@link GenericExceptionResolver}.
 *
 * <p>Verifies that each domain exception is mapped to the correct
 * {@code customNumber} in the GraphQL {@code extensions} block and that
 * unmapped exceptions fall back to code {@code 5000}.
 */
class GenericExceptionResolverTest {

    private GenericExceptionResolver resolver;
    private DataFetchingEnvironment env;

    @BeforeEach
    void setUp() {
        resolver = new GenericExceptionResolver();
        env = mock(DataFetchingEnvironment.class);

        // GraphqlErrorBuilder.newError(env) calls env.getField().getSourceLocation()
        // and env.getExecutionStepInfo().getPath().toList()
        Field field = mock(Field.class);
        when(env.getField()).thenReturn(field);

        ExecutionStepInfo stepInfo = mock(ExecutionStepInfo.class);
        ResultPath path = mock(ResultPath.class);
        when(path.toList()).thenReturn(List.of());
        when(stepInfo.getPath()).thenReturn(path);
        when(env.getExecutionStepInfo()).thenReturn(stepInfo);
    }

    @Test
    void invalidCredentials_ShouldReturnCode1001() {
        GraphQLError error = resolver.resolveToSingleError(new InvalidCredentialsException(), env);

        assertThat(error).isNotNull();
        assertThat(error.getExtensions()).containsEntry("customNumber", 1001);
        assertThat(error.getMessage()).isEqualTo("Credenciais inválidas");
    }

    @Test
    void tokenExpired_ShouldReturnCode1002() {
        GraphQLError error = resolver.resolveToSingleError(new TokenExpiredException(), env);

        assertThat(error).isNotNull();
        assertThat(error.getExtensions()).containsEntry("customNumber", 1002);
        assertThat(error.getMessage()).isEqualTo("Token expirado/inválido");
    }

    @Test
    void emailAlreadyExists_ShouldReturnCode2001() {
        GraphQLError error = resolver.resolveToSingleError(new EmailAlreadyExistsException(), env);

        assertThat(error).isNotNull();
        assertThat(error.getExtensions()).containsEntry("customNumber", 2001);
        assertThat(error.getMessage()).isEqualTo("E-mail já cadastrado");
    }

    @Test
    void mediaAlreadyExist_ShouldReturnCode2003() {
        GraphQLError error = resolver.resolveToSingleError(new MediaAlreadyExist("media"), env);

        assertThat(error).isNotNull();
        assertThat(error.getExtensions()).containsEntry("customNumber", 2003);
        assertThat(error.getMessage()).isEqualTo("Mídia já existe");
    }

    @Test
    void inputValidation_ShouldReturnCode2004() {
        GraphQLError error = resolver.resolveToSingleError(new InputValidationException(), env);

        assertThat(error).isNotNull();
        assertThat(error.getExtensions()).containsEntry("customNumber", 2004);
        assertThat(error.getMessage()).isEqualTo("Validação de input falhou");
    }

    @Test
    void jakartaValidationException_ShouldReturnCode2004() {
        GraphQLError error = resolver.resolveToSingleError(new ValidationException("invalid"), env);

        assertThat(error).isNotNull();
        assertThat(error.getExtensions()).containsEntry("customNumber", 2004);
        assertThat(error.getMessage()).isEqualTo("Validação de input falhou");
    }

    @Test
    void genericException_ShouldReturnCode5000WithOriginalMessage() {
        GraphQLError error = resolver.resolveToSingleError(new GenericException("Media not found"), env);

        assertThat(error).isNotNull();
        assertThat(error.getExtensions()).containsEntry("customNumber", 5000);
        assertThat(error.getMessage()).isEqualTo("Media not found");
    }

    @Test
    void dataAccessException_ShouldReturnCode5001() {
        GraphQLError error = resolver.resolveToSingleError(
                new org.springframework.dao.DataIntegrityViolationException("DB error"), env);

        assertThat(error).isNotNull();
        assertThat(error.getExtensions()).containsEntry("customNumber", 5001);
        assertThat(error.getMessage()).isEqualTo("Erro de banco de dados");
    }

    @Test
    void unmappedException_ShouldReturnCode5000WithGenericMessage() {
        GraphQLError error = resolver.resolveToSingleError(new RuntimeException("unexpected"), env);

        assertThat(error).isNotNull();
        assertThat(error.getExtensions()).containsEntry("customNumber", 5000);
        assertThat(error.getMessage()).isEqualTo("Erro inesperado do servidor");
    }

    @Test
    void nullPointerException_ShouldReturnCode5000() {
        GraphQLError error = resolver.resolveToSingleError(new NullPointerException(), env);

        assertThat(error).isNotNull();
        assertThat(error.getExtensions()).containsEntry("customNumber", 5000);
    }

    @Test
    void allBusinessErrors_ShouldNotContainStacktrace() {
        // Business exceptions must never expose a stack trace – only message + customNumber
        GraphQLError error1001 = resolver.resolveToSingleError(new InvalidCredentialsException(), env);
        GraphQLError error1002 = resolver.resolveToSingleError(new TokenExpiredException(), env);
        GraphQLError error2001 = resolver.resolveToSingleError(new EmailAlreadyExistsException(), env);
        GraphQLError error2003 = resolver.resolveToSingleError(new MediaAlreadyExist("x"), env);
        GraphQLError error2004 = resolver.resolveToSingleError(new InputValidationException(), env);

        for (GraphQLError error : new GraphQLError[]{error1001, error1002, error2001, error2003, error2004}) {
            assertThat(error.getExtensions()).doesNotContainKey("stacktrace");
            assertThat(error.getExtensions()).doesNotContainKey("exception");
        }
    }
}
