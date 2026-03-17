package com.espacogeek.geek.query.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ActiveProfiles;

import com.espacogeek.geek.config.JwtConfig;
import com.espacogeek.geek.controllers.UserController;
import com.espacogeek.geek.models.UserModel;
import com.espacogeek.geek.services.JwtTokenService;
import com.espacogeek.geek.services.UserService;
import com.espacogeek.geek.services.EmailService;
import com.espacogeek.geek.services.EmailVerificationService;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * Tests for the login GraphQL mutation.
 * The mutation returns an AuthPayload { accessToken, user { id, username, email } }.
 * The refresh token is delivered via an HttpOnly cookie (not tested in @GraphQlTest scope).
 */
@GraphQlTest(UserController.class)
@ActiveProfiles("test")
@SuppressWarnings("null")
class LoginQueryTest {

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

    @Test
    void login_ValidCredentials_ShouldReturnAccessToken() {
        // Given
        String email = "user@example.com";
        String password = "ValidPassword123!";
        String hashedPassword = new String(BCrypt.withDefaults().hash(12, password.toCharArray()));
        String expectedAccessToken = "access.jwt.token.here";

        UserModel user = new UserModel();
        user.setId(1);
        user.setUsername("testuser");
        user.setEmail(email);
        user.setPassword(hashedPassword.getBytes());

        when(userService.findUserByEmail(email)).thenReturn(Optional.of(user));
        when(jwtConfig.generateAccessToken(any(UserModel.class))).thenReturn(expectedAccessToken);
        when(jwtConfig.generateRefreshToken(any(UserModel.class))).thenReturn("refresh.jwt.token.here");
        when(jwtTokenService.saveToken(anyString(), any(UserModel.class), any())).thenReturn(null);

        // When & Then
        graphQlTester.document("""
                mutation {
                    login(email: "%s", password: "%s") {
                        accessToken
                        user {
                            id
                            username
                            email
                        }
                    }
                }
                """.formatted(email, password))
                .execute()
                .path("login.accessToken")
                .entity(String.class)
                .satisfies(token -> {
                    assertThat(token).isEqualTo(expectedAccessToken);
                });
    }

    @Test
    void login_ValidCredentials_ShouldReturnUserData() {
        // Given
        String email = "user@example.com";
        String password = "ValidPassword123!";
        String hashedPassword = new String(BCrypt.withDefaults().hash(12, password.toCharArray()));

        UserModel user = new UserModel();
        user.setId(1);
        user.setUsername("testuser");
        user.setEmail(email);
        user.setPassword(hashedPassword.getBytes());

        when(userService.findUserByEmail(email)).thenReturn(Optional.of(user));
        when(jwtConfig.generateAccessToken(any(UserModel.class))).thenReturn("access.token");
        when(jwtConfig.generateRefreshToken(any(UserModel.class))).thenReturn("refresh.token");
        when(jwtTokenService.saveToken(anyString(), any(UserModel.class), any())).thenReturn(null);

        // When & Then
        graphQlTester.document("""
                mutation {
                    login(email: "%s", password: "%s") {
                        accessToken
                        user {
                            id
                            username
                            email
                        }
                    }
                }
                """.formatted(email, password))
                .execute()
                .path("login.user.email")
                .entity(String.class)
                .satisfies(returnedEmail -> {
                    assertThat(returnedEmail).isEqualTo(email);
                });
    }

    @Test
    void login_InvalidEmail_ShouldReturnError() {
        // Given
        String email = "nonexistent@example.com";
        String password = "password123";

        when(userService.findUserByEmail(email)).thenReturn(Optional.empty());

        // When & Then
        graphQlTester.document("""
                mutation {
                    login(email: "%s", password: "%s") {
                        accessToken
                    }
                }
                """.formatted(email, password))
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors).isNotEmpty();
                });
    }

    @Test
    void login_InvalidPassword_ShouldReturnError() {
        // Given
        String email = "user@example.com";
        String correctPassword = "CorrectPassword123!";
        String wrongPassword = "WrongPassword123!";
        String hashedPassword = new String(BCrypt.withDefaults().hash(12, correctPassword.toCharArray()));

        UserModel user = new UserModel();
        user.setId(1);
        user.setUsername("testuser");
        user.setEmail(email);
        user.setPassword(hashedPassword.getBytes());

        when(userService.findUserByEmail(email)).thenReturn(Optional.of(user));

        // When & Then
        graphQlTester.document("""
                mutation {
                    login(email: "%s", password: "%s") {
                        accessToken
                    }
                }
                """.formatted(email, wrongPassword))
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors).isNotEmpty();
                });
    }
}
