# Email Implementation Documentation

## Overview

This document describes the email implementation for the EspacoGeek API, including configuration, usage, and available email workflows.

## Features

The email system provides confirmation emails for the following user actions:

1. **Account Creation** - Sends verification email when a new user registers
2. **Password Recovery** - Sends password reset link via email
3. **Email Change** - Sends verification email to the new email address
4. **Password Change** - Sends confirmation email after password update

## Configuration

### Email Server Settings

Add the following environment variables to your `.env` file:

```properties
# Email Configuration
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
APP_NAME=EspacoGeek
FRONTEND_URL=http://localhost:3000
```

### Gmail Setup

If using Gmail:

1. Enable 2-Factor Authentication on your Google account
2. Generate an App Password:
   - Go to Google Account Settings → Security → 2-Step Verification → App Passwords
   - Select "Mail" and your device
   - Copy the generated 16-character password
3. Use the App Password as `MAIL_PASSWORD`

### Other Email Providers

For other SMTP providers, update `MAIL_HOST` and `MAIL_PORT` accordingly:

- **SendGrid**: `smtp.sendgrid.net:587`
- **Mailgun**: `smtp.mailgun.org:587`
- **Amazon SES**: `email-smtp.[region].amazonaws.com:587`

## GraphQL API

### 1. Create Account with Email Verification

```graphql
mutation {
  createUser(credentials: {
    username: "johndoe"
    email: "john@example.com"
    password: "SecurePass123!"
  })
}
```

**Email Sent:** Account verification email with 24-hour token

**Frontend Flow:**
1. User receives email with verification link: `{FRONTEND_URL}/verify-account?token={token}`
2. Frontend can display verification status (optional - API doesn't enforce verification before login currently)

### 2. Request Password Reset

```graphql
mutation {
  requestPasswordReset(email: "john@example.com")
}
```

**Email Sent:** Password reset email with 1-hour token

**Frontend Flow:**
1. User receives email with reset link: `{FRONTEND_URL}/reset-password?token={token}`
2. Frontend displays password reset form
3. Frontend calls `resetPassword` mutation

### 3. Reset Password

```graphql
mutation {
  resetPassword(
    token: "abc-123-token-from-email"
    newPassword: "NewSecurePass123!"
  )
}
```

**Email Sent:** Password change confirmation

**Security:**
- Token is single-use only
- Token expires after 1 hour
- Sends confirmation email to notify user of password change

### 4. Request Email Change

```graphql
mutation {
  editEmail(
    password: "CurrentPassword123!"
    newEmail: "newemail@example.com"
  )
}
```

**Requires:** Authentication (JWT token)

**Email Sent:** Verification email to NEW email address with 24-hour token

**Frontend Flow:**
1. User receives email at NEW address with verification link: `{FRONTEND_URL}/verify-email-change?token={token}`
2. Frontend calls `verifyEmailChange` mutation

### 5. Verify Email Change

```graphql
mutation {
  verifyEmailChange(token: "abc-123-token-from-email")
}
```

**Requires:** Authentication (must be the same user who requested the change)

**Effect:** Updates user's email address to the new email

### 6. Change Password

```graphql
mutation {
  editPassword(
    actualPassword: "CurrentPassword123!"
    newPassword: "NewPassword123!"
  )
}
```

**Requires:** Authentication (JWT token)

**Email Sent:** Password change confirmation

## Email Templates

All emails use beautiful HTML templates with:

- Responsive design for mobile and desktop
- Gradient header with brand colors (purple/blue)
- Clear call-to-action buttons
- Alternative plain text links for accessibility
- Professional footer with copyright and timestamp

### Template Structure

```html
<!DOCTYPE html>
<html>
  <head>
    <!-- Responsive meta tags and styles -->
  </head>
  <body>
    <div class="email-container">
      <div class="header">
        <!-- App name with gradient background -->
      </div>
      <div class="content">
        <div class="greeting">Hello {username},</div>
        <div class="message">{message}</div>
        <div class="button-container">
          <a href="{actionUrl}" class="action-button">{actionText}</a>
        </div>
        <div class="link-alternative">
          <!-- Plain text link as fallback -->
        </div>
        <div class="footer-note">{footer}</div>
      </div>
      <div class="footer">
        <!-- Copyright and automated message notice -->
      </div>
    </div>
  </body>
</html>
```

## Token Management

### Token Types

- `ACCOUNT_VERIFICATION` - For new account email verification (24h expiry)
- `PASSWORD_RESET` - For password recovery (1h expiry)
- `EMAIL_CHANGE` - For email change verification (24h expiry)

### Token Properties

- **Format:** UUID (e.g., `550e8400-e29b-41d4-a716-446655440000`)
- **Storage:** Database table `email_verification_tokens`
- **Security:** Single-use only, automatically invalidated after use
- **Cleanup:** Expired tokens can be cleaned up using `EmailVerificationService.deleteExpiredTokens()`

### Token Validation

Tokens are validated for:
1. Existence in database
2. Correct token type
3. Not expired
4. Not already used
5. Associated with correct user (for authenticated operations)

## Database Schema

### email_verification_tokens Table

```sql
CREATE TABLE `email_verification_tokens` (
  `id_token` INT NOT NULL AUTO_INCREMENT,
  `token` VARCHAR(255) NOT NULL,
  `user_id` INT NOT NULL,
  `token_type` VARCHAR(50) NOT NULL,
  `new_email` VARCHAR(50) NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `expires_at` TIMESTAMP NOT NULL,
  `used` BOOLEAN DEFAULT FALSE,
  PRIMARY KEY (`id_token`),
  UNIQUE KEY `UK_token` (`token`),
  KEY `FK_email_verification_user` (`user_id`),
  CONSTRAINT `FK_email_verification_user` 
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id_user`) 
    ON DELETE CASCADE
);
```

## Service Layer

### EmailService

```java
public interface EmailService {
    void sendAccountVerificationEmail(UserModel user, String token);
    void sendPasswordResetEmail(UserModel user, String token);
    void sendEmailChangeVerificationEmail(UserModel user, String newEmail, String token);
    void sendPasswordChangeConfirmationEmail(UserModel user);
}
```

### EmailVerificationService

```java
public interface EmailVerificationService {
    EmailVerificationTokenModel createToken(UserModel user, String tokenType, 
                                           String newEmail, int expirationHours);
    Optional<EmailVerificationTokenModel> validateToken(String token, String tokenType);
    void markTokenAsUsed(EmailVerificationTokenModel token);
    void deleteExpiredTokens();
    void deleteUserTokensByType(UserModel user, String tokenType);
}
```

## Error Handling

### Common Errors

| Error | Cause | Solution |
|-------|-------|----------|
| "Failed to send email" | SMTP configuration issue | Check email credentials and server settings |
| Token not found | Invalid or expired token | Request a new token |
| Token already used | Attempting to reuse a token | Request a new token |
| Token expired | Token older than expiration time | Request a new token |
| Unauthorized | User doesn't match token | Ensure authenticated user is the token owner |

## Testing

### Unit Tests

All email functionality is covered by unit tests:

- `CreateUserMutationTest` - Account creation with email
- `EditPasswordMutationTest` - Password change confirmation
- `EditEmailMutationTest` - Email change verification flow
- `PasswordResetMutationTest` - Password reset workflow

### Manual Testing

Use GraphiQL at `http://localhost:8080/graphiql` to test mutations.

### Email Testing Tools

For development, consider using:

- **Mailtrap** - Email testing service that catches all emails
- **MailHog** - Local email testing tool
- **Gmail** - For production-like testing

## Production Considerations

### Security

1. **Use HTTPS** - Ensure frontend URLs use HTTPS in production
2. **Rate Limiting** - Implement rate limiting on password reset requests
3. **Monitoring** - Monitor failed email sends
4. **Secrets Management** - Never commit email credentials to version control

### Performance

1. **Async Sending** - Consider making email sending asynchronous for better performance
2. **Queue System** - For high volume, use a message queue (RabbitMQ, Kafka)
3. **Connection Pooling** - JavaMailSender handles connection pooling automatically

### Scalability

1. **Email Service Provider** - Use professional email services (SendGrid, Mailgun, Amazon SES)
2. **Templates** - Consider using a template engine like Thymeleaf for more complex emails
3. **Localization** - Add support for multiple languages if needed

## Troubleshooting

### Email Not Received

1. Check spam/junk folder
2. Verify email configuration in application logs
3. Check SMTP server status
4. Verify firewall doesn't block port 587

### Token Issues

1. Check token expiration time
2. Verify token hasn't been used already
3. Ensure user ID matches for authenticated operations
4. Check database for token existence

### SMTP Authentication Failed

1. Verify credentials are correct
2. Check if 2FA is enabled (use App Password for Gmail)
3. Verify SMTP server allows connections from your IP
4. Check for rate limiting from email provider

## Future Enhancements

Potential improvements for the email system:

1. **Email Templates**
   - Use Thymeleaf or FreeMarker for more dynamic templates
   - Add branding customization options
   - Support for multiple languages

2. **Enhanced Security**
   - Add CAPTCHA to password reset requests
   - Implement rate limiting per email/IP
   - Add email verification requirement before login

3. **User Experience**
   - Resend verification email option
   - Email preferences management
   - Email notification settings

4. **Analytics**
   - Track email open rates
   - Monitor click-through rates
   - Log email delivery success/failure

5. **Advanced Features**
   - Two-factor authentication via email
   - Magic link login (passwordless)
   - Email digest notifications
   - Welcome email series for new users

## Support

For issues or questions:
- Check the main README.md for project setup
- Review GraphQL schema documentation at `/graphiql`
- Check application logs for detailed error messages
