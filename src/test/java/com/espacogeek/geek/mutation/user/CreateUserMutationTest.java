package com.espacogeek.geek.mutation.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ActiveProfiles;

import com.espacogeek.geek.config.JwtConfig;
import com.espacogeek.geek.controllers.UserController;
import com.espacogeek.geek.models.EmailVerificationTokenModel;
import com.espacogeek.geek.models.UserModel;
import com.espacogeek.geek.services.EmailService;
import com.espacogeek.geek.services.EmailVerificationService;
import com.espacogeek.geek.services.JwtTokenService;
import com.espacogeek.geek.services.UserService;
import com.espacogeek.geek.utils.TokenUtils;

@GraphQlTest(UserController.class)
@ActiveProfiles("test")
@SuppressWarnings("null")
class CreateUserMutationTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtConfig jwtConfig;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private EmailVerificationService emailVerificationService;

    // Mock necessário para satisfazer a dependência do UserController
    @MockitoBean
    private TokenUtils tokenUtils;


    @Test
    void createUser_ValidCredentials_ShouldReturnCreatedStatus() {
        // Given
        String username = "newuser";
        String email = "newuser@example.com";
        String password = "ValidPassword123!";

        UserModel savedUser = new UserModel();
        savedUser.setId(1);
        savedUser.setUsername(username);
        savedUser.setEmail(email);

        EmailVerificationTokenModel token = new EmailVerificationTokenModel();
        token.setToken("test-token");

        when(userService.save(any(UserModel.class))).thenReturn(savedUser);
        when(emailVerificationService.createToken(any(UserModel.class), eq("ACCOUNT_VERIFICATION"), eq(null), anyInt()))
            .thenReturn(token);

        // When & Then
        graphQlTester.document("""
                mutation {
                    createUser(credentials: {
                        username: "%s",
                        email: "%s",
                        password: "%s"
                    })
                }
                """.formatted(username, email, password))
                .execute()
                .path("createUser")
                .entity(String.class)
                .satisfies(status -> {
                    assertThat(status).contains("201");
                });

        verify(userService).save(any(UserModel.class));
        verify(emailVerificationService).createToken(any(UserModel.class), eq("ACCOUNT_VERIFICATION"), eq(null), anyInt());
        verify(emailService).sendAccountVerificationEmail(any(UserModel.class), anyString());
    }

    @Test
    void createUser_ShortPassword_ShouldReturnError() {
        // Given
        String username = "newuser";
        String email = "newuser@example.com";
        String shortPassword = "short";

        // When & Then
        graphQlTester.document("""
                mutation {
                    createUser(credentials: {
                        username: "%s",
                        email: "%s",
                        password: "%s"
                    })
                }
                """.formatted(username, email, shortPassword))
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors).isNotEmpty();
                });
    }

    @Test
    void createUser_ShortUsername_ShouldReturnError() {
        // Given
        String shortUsername = "ab";
        String email = "newuser@example.com";
        String password = "validPassword123";

        // When & Then
        graphQlTester.document("""
                mutation {
                    createUser(credentials: {
                        username: "%s",
                        email: "%s",
                        password: "%s"
                    })
                }
                """.formatted(shortUsername, email, password))
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors).isNotEmpty();
                });
    }

    @Test
    void createUser_LongUsername_ShouldReturnError() {
        // Given
        String longUsername = "a".repeat(22);
        String email = "newuser@example.com";
        String password = "ValidPassword123!";

        // When & Then
        graphQlTester.document("""
                mutation {
                    createUser(credentials: {
                        username: "%s",
                        email: "%s",
                        password: "%s"
                    })
                }
                """.formatted(longUsername, email, password))
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors).isNotEmpty();
                });
    }
}
