package com.espacogeek.geek.services.impl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.espacogeek.geek.models.JwtTokenModel;
import com.espacogeek.geek.models.UserModel;
import com.espacogeek.geek.repositories.JwtTokenRepository;
import com.espacogeek.geek.services.JwtTokenService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Implementation of JwtTokenService.
 */
@Service
public class JwtTokenServiceImpl implements JwtTokenService {

    @Autowired
    private JwtTokenRepository jwtTokenRepository;

    @Value("${security.jwt.secret:ZmFrZS1zZWNyZXQtZmFrZS1zZWNyZXQtZmFrZS1zZWNyZXQtMTIzNDU2}")
    private String secret;

    @Value("${security.jwt.issuer:espaco-geek}")
    private String issuer;

    private SecretKey getSigningKey() {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException ex) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    @Transactional
    public JwtTokenModel saveToken(String token, UserModel user, String deviceInfo) {
        // Extract expiration from token
        LocalDateTime expiresAt = extractExpiration(token);
        
        JwtTokenModel jwtToken = new JwtTokenModel();
        jwtToken.setToken(token);
        jwtToken.setUser(user);
        jwtToken.setCreatedAt(LocalDateTime.now());
        jwtToken.setExpiresAt(expiresAt);
        jwtToken.setDeviceInfo(deviceInfo);
        
        return jwtTokenRepository.save(jwtToken);
    }

    @Override
    public boolean isTokenValid(String token) {
        Optional<JwtTokenModel> tokenModel = jwtTokenRepository.findByToken(token);
        if (tokenModel.isEmpty()) {
            return false;
        }
        
        // Check if token is expired
        return tokenModel.get().getExpiresAt().isAfter(LocalDateTime.now());
    }

    @Override
    public Optional<JwtTokenModel> findByToken(String token) {
        return jwtTokenRepository.findByToken(token);
    }

    @Override
    public List<JwtTokenModel> findByUserId(Integer userId) {
        return jwtTokenRepository.findByUserId(userId);
    }

    @Override
    @Transactional
    public void deleteToken(String token) {
        jwtTokenRepository.findByToken(token).ifPresent(jwtTokenRepository::delete);
    }

    @Override
    @Transactional
    public void deleteUserTokens(Integer userId) {
        jwtTokenRepository.deleteByUserId(userId);
    }

    @Override
    @Transactional
    public int cleanupExpiredTokens() {
        return jwtTokenRepository.deleteExpiredTokens(LocalDateTime.now());
    }

    /**
     * Extract expiration date from JWT token.
     */
    private LocalDateTime extractExpiration(String token) {
        try {
            Claims claims = Jwts.parser()
                    .requireIssuer(issuer)
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            Date expiration = claims.getExpiration();
            return Instant.ofEpochMilli(expiration.getTime())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
        } catch (io.jsonwebtoken.JwtException e) {
            // If we can't parse the token, throw an exception to prevent saving invalid tokens
            throw new IllegalArgumentException("Invalid JWT token: cannot extract expiration", e);
        }
    }
}
