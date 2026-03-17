package com.espacogeek.geek.controllers;

import java.util.List;

import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;

import com.espacogeek.geek.config.JwtConfig;
import com.espacogeek.geek.exception.GenericException;
import com.espacogeek.geek.models.EmailVerificationTokenModel;
import com.espacogeek.geek.models.UserModel;
import com.espacogeek.geek.services.EmailService;
import com.espacogeek.geek.services.EmailVerificationService;
import com.espacogeek.geek.services.JwtTokenService;
import com.espacogeek.geek.services.UserService;
import com.espacogeek.geek.types.AuthPayload;
import com.espacogeek.geek.types.NewUser;
import com.espacogeek.geek.utils.UserUtils;

import at.favre.lib.crypto.bcrypt.BCrypt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final JwtConfig jwtConfig;
    private final JwtTokenService jwtTokenService;
    private final EmailService emailService;
    private final EmailVerificationService emailVerificationService;

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @QueryMapping
    public List<UserModel> findUser(@Argument(name = "id") Integer id, @Argument(name = "username") String username, @Argument(name = "email") String email) {
        return userService.findByIdOrUsernameContainsOrEmail(id, username, email);
    }

    @QueryMapping(name = "logout")
    @PreAuthorize("hasRole('user')")
    public String doLogoutUser(
            Authentication authentication,
            @CookieValue(name = "refreshToken", required = false) String refreshTokenCookie,
            DataFetchingEnvironment environment) {
        // Invalidate the refresh token stored in the database
        if (refreshTokenCookie != null && !refreshTokenCookie.isBlank()) {
            jwtTokenService.deleteToken(refreshTokenCookie);
        }

        // Signal the interceptor to clear the refreshToken cookie
        boolean[] clearHolder = environment.getGraphQlContext().get("clearRefreshCookieHolder");
        if (clearHolder != null) {
            clearHolder[0] = true;
        }

        return HttpStatus.OK.toString();
    }

    @QueryMapping(name = "isLogged")
    @PreAuthorize("hasRole('user')")
    public String isUserLogged(Authentication authentication) {
        // @PreAuthorize already ensures the access token is valid; just confirm.
        return HttpStatus.OK.toString();
    }

    /**
     * Authenticate with email and password.
     * Returns a short-lived access token in the JSON payload and sets a long-lived
     * refresh token as an HttpOnly cookie named {@code refreshToken}.
     */
    @MutationMapping(name = "login")
    public AuthPayload doLoginUser(
            @Argument(name = "email") String email,
            @Argument(name = "password") String password,
            @Argument(name = "deviceInfo") String deviceInfo,
            DataFetchingEnvironment environment) {

        UserModel user = userService.findUserByEmail(email)
                .orElseThrow(() -> new GenericException(HttpStatus.UNAUTHORIZED.toString()));

        boolean verified = BCrypt.verifyer().verify(password.toCharArray(), user.getPassword()).verified;
        if (!verified) {
            throw new GenericException(HttpStatus.UNAUTHORIZED.toString());
        }

        // Short-lived access token — returned in the JSON payload
        String accessToken = jwtConfig.generateAccessToken(user);

        // Long-lived refresh token — stored in DB and delivered via HttpOnly cookie
        String refreshToken = jwtConfig.generateRefreshToken(user);
        jwtTokenService.saveToken(refreshToken, user, deviceInfo);

        // Queue the refresh token cookie via the shared GraphQL context container
        queueRefreshTokenCookie(environment, refreshToken);

        return new AuthPayload(accessToken, user);
    }

    /**
     * Obtain a new access token using the refresh token from the {@code refreshToken} cookie.
     * Implements token rotation: the old refresh token is invalidated and a new one is issued.
     */
    @MutationMapping(name = "refreshToken")
    public AuthPayload doRefreshToken(
            @CookieValue(name = "refreshToken", required = false) String refreshTokenCookie,
            DataFetchingEnvironment environment) {

        if (refreshTokenCookie == null || refreshTokenCookie.isBlank()) {
            throw new GenericException(HttpStatus.UNAUTHORIZED.toString());
        }

        // Validate JWT signature and type claim
        var claims = jwtConfig.validate(refreshTokenCookie);
        if (claims == null) {
            throw new GenericException(HttpStatus.UNAUTHORIZED.toString());
        }
        Object tokenType = claims.get("type");
        if (!"refresh".equals(tokenType)) {
            throw new GenericException(HttpStatus.UNAUTHORIZED.toString());
        }

        // Validate that the token is still present and not expired in the database
        if (!jwtTokenService.isTokenValid(refreshTokenCookie)) {
            throw new GenericException(HttpStatus.UNAUTHORIZED.toString());
        }

        String email = claims.getSubject();
        UserModel user = userService.findUserByEmail(email)
                .orElseThrow(() -> new GenericException(HttpStatus.UNAUTHORIZED.toString()));

        // Token rotation: invalidate the used refresh token and issue a new one
        jwtTokenService.deleteToken(refreshTokenCookie);
        String newRefreshToken = jwtConfig.generateRefreshToken(user);
        jwtTokenService.saveToken(newRefreshToken, user, null);
        queueRefreshTokenCookie(environment, newRefreshToken);

        // Issue a new short-lived access token
        String newAccessToken = jwtConfig.generateAccessToken(user);
        return new AuthPayload(newAccessToken, user);
    }

    @MutationMapping(name = "createUser")
    public String createUser(@Argument(name = "credentials") NewUser newUser) {

        if (!UserUtils.isValidPassword(newUser.password())) {
            throw new GenericException(HttpStatus.BAD_REQUEST.toString());
        }

        if (newUser.username().trim().length() < 3 || newUser.username().trim().length() > 21) {
            throw new GenericException(HttpStatus.BAD_REQUEST.toString());
        }

        byte[] passwordCrypt = BCrypt.withDefaults().hash(12, newUser.password().toCharArray());
        UserModel user = new UserModel();
        user.setUsername(newUser.username().trim());
        user.setEmail(newUser.email().toLowerCase().trim());
        user.setPassword(passwordCrypt);

        userService.save(user);

        // Send verification email
        EmailVerificationTokenModel token = emailVerificationService.createToken(user, "ACCOUNT_VERIFICATION", null, 24);
        emailService.sendAccountVerificationEmail(user, token.getToken());

        return HttpStatus.CREATED.toString();
    }

    @MutationMapping(name = "editPassword")
    @PreAuthorize("hasRole('user')")
    public String editPasswordUserLogged(Authentication authentication, @Argument(name = "actualPassword") String actualPassword, @Argument(name = "newPassword") String newPassword) {

        Integer userId = UserUtils.getUserID(authentication);

        UserModel userLogged = userService.findById(Integer.valueOf(userId)).get();
        boolean resultPassword = BCrypt.verifyer().verify(actualPassword.toCharArray(), userLogged.getPassword()).verified;

        if (resultPassword) {
            userLogged.setPassword(BCrypt.withDefaults().hash(12, newPassword.toCharArray()));
            userService.save(userLogged);

            // Send confirmation email
            emailService.sendPasswordChangeConfirmationEmail(userLogged);

            return HttpStatus.OK.toString();
        }

        throw new GenericException(HttpStatus.UNAUTHORIZED.toString());
    }

    @MutationMapping(name = "deleteUser")
    @PreAuthorize("hasRole('user')")
    public String deleteUserLogged(Authentication authentication, @Argument(name = "password") String password) {

        Integer userId = UserUtils.getUserID(authentication);

        UserModel userLogged = userService.findById(userId).get();
        boolean resultPassword = BCrypt.verifyer().verify(password.toCharArray(), userLogged.getPassword()).verified;

        if (resultPassword) {
            userService.deleteById(Integer.valueOf(userId));
            return HttpStatus.OK.toString();
        }

        throw new GenericException(HttpStatus.UNAUTHORIZED.toString());
    }

    @MutationMapping(name = "editUsername")
    @PreAuthorize("hasRole('user')")
    public String editUsernameUserLogged(Authentication authentication, @Argument(name = "password") String password, @Argument(name = "newUsername") String newUsername) {

        Integer userId = UserUtils.getUserID(authentication);

        UserModel userLogged = userService.findById(userId).get();
        boolean resultPassword = BCrypt.verifyer().verify(password.toCharArray(), userLogged.getPassword()).verified;

        if (resultPassword) {
            userLogged.setUsername(newUsername);
            userService.save(userLogged);
            return HttpStatus.OK.toString();
        }

        throw new GenericException(HttpStatus.UNAUTHORIZED.toString());
    }

    @MutationMapping(name = "editEmail")
    @PreAuthorize("hasRole('user')")
    public String editEmailUserLogged(Authentication authentication, @Argument(name = "password") String password, @Argument(name = "newEmail") String newEmail) {

        Integer userId = UserUtils.getUserID(authentication);

        UserModel userLogged = userService.findById(userId).get();
        boolean resultPassword = BCrypt.verifyer().verify(password.toCharArray(), userLogged.getPassword()).verified;

        if (resultPassword) {
            // Create verification token for new email
            EmailVerificationTokenModel token = emailVerificationService.createToken(userLogged, "EMAIL_CHANGE", newEmail, 24);
            emailService.sendEmailChangeVerificationEmail(userLogged, newEmail, token.getToken());

            return HttpStatus.OK.toString();
        }

        throw new GenericException(HttpStatus.UNAUTHORIZED.toString());
    }

    @MutationMapping(name = "requestPasswordReset")
    public String requestPasswordReset(@Argument(name = "email") String email) {
        UserModel user = userService.findUserByEmail(email).orElseThrow(() -> new GenericException(HttpStatus.NOT_FOUND.toString()));

        // Create password reset token
        EmailVerificationTokenModel token = emailVerificationService.createToken(user, "PASSWORD_RESET", null, 1);
        emailService.sendPasswordResetEmail(user, token.getToken());

        return HttpStatus.OK.toString();
    }

    @MutationMapping(name = "resetPassword")
    public String resetPassword(@Argument(name = "token") String token, @Argument(name = "newPassword") String newPassword) {
        if (!UserUtils.isValidPassword(newPassword)) {
            throw new GenericException(HttpStatus.BAD_REQUEST.toString());
        }

        EmailVerificationTokenModel verificationToken = emailVerificationService.validateToken(token, "PASSWORD_RESET")
            .orElseThrow(() -> new GenericException(HttpStatus.UNAUTHORIZED.toString()));

        UserModel user = verificationToken.getUser();
        user.setPassword(BCrypt.withDefaults().hash(12, newPassword.toCharArray()));
        userService.save(user);

        emailVerificationService.markTokenAsUsed(verificationToken);
        emailService.sendPasswordChangeConfirmationEmail(user);

        return HttpStatus.OK.toString();
    }

    @MutationMapping(name = "verifyEmailChange")
    @PreAuthorize("hasRole('user')")
    public String verifyEmailChange(Authentication authentication, @Argument(name = "token") String token) {
        Integer userId = UserUtils.getUserID(authentication);

        EmailVerificationTokenModel verificationToken = emailVerificationService.validateToken(token, "EMAIL_CHANGE")
            .orElseThrow(() -> new GenericException(HttpStatus.UNAUTHORIZED.toString()));

        if (!verificationToken.getUser().getId().equals(userId)) {
            throw new GenericException(HttpStatus.FORBIDDEN.toString());
        }

        UserModel user = verificationToken.getUser();
        user.setEmail(verificationToken.getNewEmail());
        userService.save(user);

        emailVerificationService.markTokenAsUsed(verificationToken);

        return HttpStatus.OK.toString();
    }

    // ---------- Helpers

    /**
     * Queue the refresh token to be delivered as an HttpOnly cookie via
     * {@link GraphQlCookieInterceptor}. The interceptor injects a shared
     * {@code "pendingRefreshTokens"} list into the {@link graphql.GraphQLContext}
     * before execution; this method adds the token to that list.
     * If no list is present (e.g., in {@code @GraphQlTest} unit tests that don't
     * wire the full interceptor chain), this is a no-op.
     */
    @SuppressWarnings("unchecked")
    private void queueRefreshTokenCookie(DataFetchingEnvironment environment, String refreshToken) {
        List<String> pending = environment.getGraphQlContext().get("pendingRefreshTokens");
        if (pending != null) {
            pending.add(refreshToken);
        }
    }
}
