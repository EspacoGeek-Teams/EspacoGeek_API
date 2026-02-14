package com.espacogeek.geek.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.espacogeek.geek.models.UserModel;
import com.espacogeek.geek.services.EmailService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.name:EspacoGeek}")
    private String appName;

    @Override
    public void sendAccountVerificationEmail(UserModel user, String token) {
        String subject = "Verify Your Account - " + appName;
        String verificationUrl = frontendUrl + "/verify-account?token=" + token;
        
        String htmlContent = buildEmailTemplate(
            user.getUsername(),
            "Welcome to " + appName + "!",
            "Thank you for creating an account. Please verify your email address to activate your account.",
            verificationUrl,
            "Verify Account",
            "This link will expire in 24 hours."
        );
        
        sendHtmlEmail(user.getEmail(), subject, htmlContent);
    }

    @Override
    public void sendPasswordResetEmail(UserModel user, String token) {
        String subject = "Password Reset Request - " + appName;
        String resetUrl = frontendUrl + "/reset-password?token=" + token;
        
        String htmlContent = buildEmailTemplate(
            user.getUsername(),
            "Password Reset Request",
            "We received a request to reset your password. Click the button below to create a new password.",
            resetUrl,
            "Reset Password",
            "This link will expire in 1 hour. If you didn't request this, please ignore this email."
        );
        
        sendHtmlEmail(user.getEmail(), subject, htmlContent);
    }

    @Override
    public void sendEmailChangeVerificationEmail(UserModel user, String newEmail, String token) {
        String subject = "Email Change Verification - " + appName;
        String verificationUrl = frontendUrl + "/verify-email-change?token=" + token;
        
        String htmlContent = buildEmailTemplate(
            user.getUsername(),
            "Verify Your New Email Address",
            "You requested to change your email address. Please verify your new email address by clicking the button below.",
            verificationUrl,
            "Verify Email",
            "This link will expire in 24 hours. If you didn't request this change, please contact support."
        );
        
        sendHtmlEmail(newEmail, subject, htmlContent);
    }

    @Override
    public void sendPasswordChangeConfirmationEmail(UserModel user) {
        String subject = "Password Changed Successfully - " + appName;
        
        String htmlContent = buildNotificationTemplate(
            user.getUsername(),
            "Password Changed",
            "Your password has been changed successfully.",
            "If you didn't make this change, please contact support immediately."
        );
        
        sendHtmlEmail(user.getEmail(), subject, htmlContent);
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private String buildEmailTemplate(String username, String title, String message, 
                                      String actionUrl, String actionText, String footer) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s</title>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                        line-height: 1.6;
                        color: #333;
                        margin: 0;
                        padding: 0;
                        background-color: #f4f4f4;
                    }
                    .email-container {
                        max-width: 600px;
                        margin: 0 auto;
                        background-color: #ffffff;
                    }
                    .header {
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        padding: 40px 20px;
                        text-align: center;
                        color: #ffffff;
                    }
                    .header h1 {
                        margin: 0;
                        font-size: 28px;
                        font-weight: 600;
                    }
                    .content {
                        padding: 40px 30px;
                    }
                    .greeting {
                        font-size: 18px;
                        color: #333;
                        margin-bottom: 20px;
                    }
                    .message {
                        font-size: 16px;
                        color: #555;
                        margin-bottom: 30px;
                        line-height: 1.8;
                    }
                    .button-container {
                        text-align: center;
                        margin: 35px 0;
                    }
                    .action-button {
                        display: inline-block;
                        padding: 14px 40px;
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        color: #ffffff;
                        text-decoration: none;
                        border-radius: 5px;
                        font-size: 16px;
                        font-weight: 600;
                        transition: transform 0.2s;
                    }
                    .action-button:hover {
                        transform: translateY(-2px);
                    }
                    .footer-note {
                        font-size: 14px;
                        color: #777;
                        margin-top: 30px;
                        padding-top: 20px;
                        border-top: 1px solid #eee;
                    }
                    .footer {
                        background-color: #f8f9fa;
                        padding: 20px;
                        text-align: center;
                        font-size: 13px;
                        color: #666;
                    }
                    .link-alternative {
                        font-size: 12px;
                        color: #888;
                        margin-top: 20px;
                        word-break: break-all;
                    }
                </style>
            </head>
            <body>
                <div class="email-container">
                    <div class="header">
                        <h1>%s</h1>
                    </div>
                    <div class="content">
                        <div class="greeting">Hello %s,</div>
                        <div class="message">%s</div>
                        <div class="button-container">
                            <a href="%s" class="action-button">%s</a>
                        </div>
                        <div class="link-alternative">
                            Or copy and paste this link into your browser:<br>
                            <a href="%s">%s</a>
                        </div>
                        <div class="footer-note">%s</div>
                    </div>
                    <div class="footer">
                        <p>&copy; 2024 %s. All rights reserved.</p>
                        <p>This is an automated message, please do not reply to this email.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(title, appName, username, message, actionUrl, actionText, 
                         actionUrl, actionUrl, footer, appName);
    }

    private String buildNotificationTemplate(String username, String title, String message, String footer) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s</title>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                        line-height: 1.6;
                        color: #333;
                        margin: 0;
                        padding: 0;
                        background-color: #f4f4f4;
                    }
                    .email-container {
                        max-width: 600px;
                        margin: 0 auto;
                        background-color: #ffffff;
                    }
                    .header {
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        padding: 40px 20px;
                        text-align: center;
                        color: #ffffff;
                    }
                    .header h1 {
                        margin: 0;
                        font-size: 28px;
                        font-weight: 600;
                    }
                    .content {
                        padding: 40px 30px;
                    }
                    .greeting {
                        font-size: 18px;
                        color: #333;
                        margin-bottom: 20px;
                    }
                    .message {
                        font-size: 16px;
                        color: #555;
                        margin-bottom: 30px;
                        line-height: 1.8;
                    }
                    .footer-note {
                        font-size: 14px;
                        color: #777;
                        margin-top: 30px;
                        padding-top: 20px;
                        border-top: 1px solid #eee;
                    }
                    .footer {
                        background-color: #f8f9fa;
                        padding: 20px;
                        text-align: center;
                        font-size: 13px;
                        color: #666;
                    }
                </style>
            </head>
            <body>
                <div class="email-container">
                    <div class="header">
                        <h1>%s</h1>
                    </div>
                    <div class="content">
                        <div class="greeting">Hello %s,</div>
                        <div class="message">%s</div>
                        <div class="footer-note">%s</div>
                    </div>
                    <div class="footer">
                        <p>&copy; 2024 %s. All rights reserved.</p>
                        <p>This is an automated message, please do not reply to this email.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(title, appName, username, message, footer, appName);
    }
}
