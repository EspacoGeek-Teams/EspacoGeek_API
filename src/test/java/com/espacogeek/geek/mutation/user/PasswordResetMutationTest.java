package com.espacogeek.geek.mutation.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
class PasswordResetMutationTest {

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

    @MockitoBean
    private TokenUtils tokenUtils;

    @Test
    void requestPasswordReset_ValidEmail_ShouldReturnOkStatus() {
        // Given
        String email = "user@example.com";

        UserModel user = new UserModel();
        user.setId(1);
        user.setUsername("testuser");
        user.setEmail(email);

        EmailVerificationTokenModel token = new EmailVerificationTokenModel();
        token.setToken("reset-token-123");

        when(userService.findUserByEmail(email)).thenReturn(Optional.of(user));
        when(emailVerificationService.createToken(any(UserModel.class), eq("PASSWORD_RESET"), eq(null), anyInt()))
            .thenReturn(token);

        // When & Then
        graphQlTester.document("""
                mutation {
                    requestPasswordReset(email: "%s")
                }
                """.formatted(email))
                .execute()
                .path("requestPasswordReset")
                .entity(String.class)
                .satisfies(status -> {
                    assertThat(status).contains("200");
                });

        verify(emailVerificationService).createToken(any(UserModel.class), eq("PASSWORD_RESET"), eq(null), anyInt());
        verify(emailService).sendPasswordResetEmail(any(UserModel.class), anyString());
    }

    @Test
    void requestPasswordReset_InvalidEmail_ShouldReturnError() {
        // Given
        String email = "nonexistent@example.com";

        when(userService.findUserByEmail(email)).thenReturn(Optional.empty());

        // When & Then
        graphQlTester.document("""
                mutation {
                    requestPasswordReset(email: "%s")
                }
                """.formatted(email))
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors).isNotEmpty();
                });
    }

    @Test
    void resetPassword_ValidToken_ShouldReturnOkStatus() {
        // Given
        String token = "valid-reset-token";
        String newPassword = "NewPassword123!";

        UserModel user = new UserModel();
        user.setId(1);
        user.setUsername("testuser");
        user.setEmail("user@example.com");

        EmailVerificationTokenModel verificationToken = new EmailVerificationTokenModel();
        verificationToken.setToken(token);
        verificationToken.setUser(user);
        verificationToken.setTokenType("PASSWORD_RESET");

        when(emailVerificationService.validateToken(token, "PASSWORD_RESET"))
            .thenReturn(Optional.of(verificationToken));
        when(userService.save(any(UserModel.class))).thenReturn(user);

        // When & Then
        graphQlTester.document("""
                mutation {
                    resetPassword(token: "%s", newPassword: "%s")
                }
                """.formatted(token, newPassword))
                .execute()
                .path("resetPassword")
                .entity(String.class)
                .satisfies(status -> {
                    assertThat(status).contains("200");
                });

        verify(userService).save(any(UserModel.class));
        verify(emailVerificationService).markTokenAsUsed(any(EmailVerificationTokenModel.class));
        verify(emailService).sendPasswordChangeConfirmationEmail(any(UserModel.class));
    }

    @Test
    void resetPassword_InvalidToken_ShouldReturnError() {
        // Given
        String token = "invalid-token";
        String newPassword = "NewPassword123!";

        when(emailVerificationService.validateToken(token, "PASSWORD_RESET"))
            .thenReturn(Optional.empty());

        // When & Then
        graphQlTester.document("""
                mutation {
                    resetPassword(token: "%s", newPassword: "%s")
                }
                """.formatted(token, newPassword))
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors).isNotEmpty();
                });
    }

    @Test
    void resetPassword_WeakPassword_ShouldReturnError() {
        // Given
        String token = "valid-reset-token";
        String weakPassword = "weak";

        // When & Then
        graphQlTester.document("""
                mutation {
                    resetPassword(token: "%s", newPassword: "%s")
                }
                """.formatted(token, weakPassword))
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors).isNotEmpty();
                });
    }
}
