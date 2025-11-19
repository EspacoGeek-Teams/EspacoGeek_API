package com.espacogeek.geek.services;

import com.espacogeek.geek.models.UserModel;

/**
 * Service interface for sending emails
 */
public interface EmailService {
    
    /**
     * Send account verification email to a new user
     * @param user The user who needs to verify their account
     * @param token The verification token
     */
    void sendAccountVerificationEmail(UserModel user, String token);
    
    /**
     * Send password reset email
     * @param user The user who requested password reset
     * @param token The password reset token
     */
    void sendPasswordResetEmail(UserModel user, String token);
    
    /**
     * Send email change verification email to the new email address
     * @param user The user changing their email
     * @param newEmail The new email address
     * @param token The verification token
     */
    void sendEmailChangeVerificationEmail(UserModel user, String newEmail, String token);
    
    /**
     * Send password change confirmation email
     * @param user The user who changed their password
     */
    void sendPasswordChangeConfirmationEmail(UserModel user);
}
