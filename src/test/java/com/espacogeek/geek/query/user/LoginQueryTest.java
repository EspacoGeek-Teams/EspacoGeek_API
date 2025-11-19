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
import com.espacogeek.geek.utils.TokenUtils;
import com.espacogeek.geek.services.EmailService;
import com.espacogeek.geek.services.EmailVerificationService;

import at.favre.lib.crypto.bcrypt.BCrypt;

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

    // Necessário para satisfazer a dependência do UserController
    @MockitoBean
    private TokenUtils tokenUtils;
    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private EmailVerificationService emailVerificationService;

    @Test
    void login_ValidCredentials_ShouldReturnToken() {
        // Given
        String email = "user@example.com";
        String password = "ValidPassword123!";
        String hashedPassword = new String(BCrypt.withDefaults().hash(12, password.toCharArray()));
        String expectedToken = "jwt.token.here";

        UserModel user = new UserModel();
        user.setId(1);
        user.setUsername("testuser");
        user.setEmail(email);
        user.setPassword(hashedPassword.getBytes());

        when(userService.findUserByEmail(email)).thenReturn(Optional.of(user));
        when(jwtConfig.generateToken(any(UserModel.class))).thenReturn(expectedToken);
        when(jwtTokenService.saveToken(anyString(), any(UserModel.class), anyString())).thenReturn(null);

        // When & Then
        graphQlTester.document("""
                query {
                    login(email: "%s", password: "%s")
                }
                """.formatted(email, password))
                .execute()
                .path("login")
                .entity(String.class)
                .satisfies(token -> {
                    assertThat(token).isEqualTo(expectedToken);
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
                query {
                    login(email: "%s", password: "%s")
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
                query {
                    login(email: "%s", password: "%s")
                }
                """.formatted(email, wrongPassword))
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors).isNotEmpty();
                });
    }
}
