package com.espacogeek.geek.services.impl;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.espacogeek.geek.models.EmailVerificationTokenModel;
import com.espacogeek.geek.models.UserModel;
import com.espacogeek.geek.repositories.EmailVerificationTokenRepository;
import com.espacogeek.geek.services.EmailVerificationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationServiceImpl.class);

    @Autowired
    private EmailVerificationTokenRepository tokenRepository;

    @Override
    @Transactional
    public EmailVerificationTokenModel createToken(UserModel user, String tokenType, String newEmail, int expirationHours) {
        // Delete existing tokens of the same type for this user
        deleteUserTokensByType(user, tokenType);
        
        EmailVerificationTokenModel token = new EmailVerificationTokenModel();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setTokenType(tokenType);
        token.setNewEmail(newEmail);
        token.setCreatedAt(LocalDateTime.now());
        token.setExpiresAt(LocalDateTime.now().plusHours(expirationHours));
        token.setUsed(false);
        
        return tokenRepository.save(token);
    }

    @Override
    public Optional<EmailVerificationTokenModel> validateToken(String token, String tokenType) {
        Optional<EmailVerificationTokenModel> tokenOpt = tokenRepository.findByTokenAndTokenType(token, tokenType);
        
        if (tokenOpt.isEmpty()) {
            log.warn("Token not found or type mismatch: {}", token);
            return Optional.empty();
        }
        
        EmailVerificationTokenModel tokenModel = tokenOpt.get();
        
        if (tokenModel.getUsed()) {
            log.warn("Token already used: {}", token);
            return Optional.empty();
        }
        
        if (tokenModel.isExpired()) {
            log.warn("Token expired: {}", token);
            return Optional.empty();
        }
        
        return Optional.of(tokenModel);
    }

    @Override
    @Transactional
    public void markTokenAsUsed(EmailVerificationTokenModel token) {
        token.setUsed(true);
        tokenRepository.save(token);
    }

    @Override
    @Transactional
    public void deleteExpiredTokens() {
        tokenRepository.deleteExpiredTokens(LocalDateTime.now());
        log.info("Deleted expired tokens");
    }

    @Override
    @Transactional
    public void deleteUserTokensByType(UserModel user, String tokenType) {
        tokenRepository.deleteByUserAndTokenType(user, tokenType);
    }
}
