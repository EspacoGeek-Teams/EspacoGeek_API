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
import org.springframework.security.test.context.support.WithMockUser;
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

import at.favre.lib.crypto.bcrypt.BCrypt;

@GraphQlTest(UserController.class)
@ActiveProfiles("test")
@SuppressWarnings("null")
class EditEmailMutationTest {

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

    // Necessário para satisfazer a dependência do UserController
    @MockitoBean
    private TokenUtils tokenUtils;

    @Test
    @WithMockUser(authorities = {"ROLE_user", "ID_1"})
    void editEmail_ValidPassword_ShouldReturnOkStatus() {
        // Given
        String password = "ValidPassword123!";
        String newEmail = "newemail@example.com";
        String hashedPassword = new String(BCrypt.withDefaults().hash(12, password.toCharArray()));

        UserModel user = new UserModel();
        user.setId(1);
        user.setUsername("testuser");
        user.setEmail("old@example.com");
        user.setPassword(hashedPassword.getBytes());

        EmailVerificationTokenModel token = new EmailVerificationTokenModel();
        token.setToken("email-change-token");

        when(userService.findById(anyInt())).thenReturn(Optional.of(user));
        when(emailVerificationService.createToken(any(UserModel.class), eq("EMAIL_CHANGE"), eq(newEmail), anyInt()))
            .thenReturn(token);

        // When & Then
        graphQlTester.document("""
                mutation {
                    editEmail(password: "%s", newEmail: "%s")
                }
                """.formatted(password, newEmail))
                .execute()
                .path("editEmail")
                .entity(String.class)
                .satisfies(status -> {
                    assertThat(status).contains("200");
                });

        verify(emailVerificationService).createToken(any(UserModel.class), eq("EMAIL_CHANGE"), eq(newEmail), anyInt());
        verify(emailService).sendEmailChangeVerificationEmail(any(UserModel.class), eq(newEmail), anyString());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_user", "ID_1"})
    void editEmail_InvalidPassword_ShouldReturnError() {
        // Given
        String correctPassword = "CorrectPassword123!";
        String wrongPassword = "WrongPassword123!";
        String newEmail = "newemail@example.com";
        String hashedPassword = new String(BCrypt.withDefaults().hash(12, correctPassword.toCharArray()));

        UserModel user = new UserModel();
        user.setId(1);
        user.setUsername("testuser");
        user.setEmail("old@example.com");
        user.setPassword(hashedPassword.getBytes());

        when(userService.findById(anyInt())).thenReturn(Optional.of(user));

        // When & Then
        graphQlTester.document("""
                mutation {
                    editEmail(password: "%s", newEmail: "%s")
                }
                """.formatted(wrongPassword, newEmail))
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors).isNotEmpty();
                });
    }
}
