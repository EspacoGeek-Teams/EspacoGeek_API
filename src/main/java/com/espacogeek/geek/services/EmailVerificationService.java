package com.espacogeek.geek.services;

import com.espacogeek.geek.models.EmailVerificationTokenModel;
import com.espacogeek.geek.models.UserModel;

import java.util.Optional;

/**
 * Service for managing email verification tokens
 */
public interface EmailVerificationService {
    
    /**
     * Create a new verification token for a user
     * @param user The user
     * @param tokenType The type of token (e.g., "ACCOUNT_VERIFICATION", "PASSWORD_RESET", "EMAIL_CHANGE")
     * @param newEmail The new email (for email change requests), null otherwise
     * @param expirationHours Token expiration time in hours
     * @return The created token model
     */
    EmailVerificationTokenModel createToken(UserModel user, String tokenType, String newEmail, int expirationHours);
    
    /**
     * Validate and retrieve a token
     * @param token The token string
     * @param tokenType The expected token type
     * @return Optional containing the token if valid
     */
    Optional<EmailVerificationTokenModel> validateToken(String token, String tokenType);
    
    /**
     * Mark a token as used
     * @param token The token to mark as used
     */
    void markTokenAsUsed(EmailVerificationTokenModel token);
    
    /**
     * Delete expired tokens (cleanup task)
     */
    void deleteExpiredTokens();
    
    /**
     * Delete all tokens for a user of a specific type
     * @param user The user
     * @param tokenType The token type
     */
    void deleteUserTokensByType(UserModel user, String tokenType);
}
