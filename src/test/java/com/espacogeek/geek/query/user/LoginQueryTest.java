package com.espacogeek.geek.query.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ActiveProfiles;

import com.espacogeek.geek.config.JwtConfig;
import com.espacogeek.geek.controllers.UserController;
import com.espacogeek.geek.models.UserModel;
import com.espacogeek.geek.services.UserService;

import at.favre.lib.crypto.bcrypt.BCrypt;

@GraphQlTest(UserController.class)
@ActiveProfiles("test")
class LoginQueryTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtConfig jwtConfig;

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
        when(userService.save(any(UserModel.class))).thenReturn(user);

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
