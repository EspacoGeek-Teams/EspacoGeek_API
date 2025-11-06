package com.espacogeek.geek.services;

import java.util.List;
import java.util.Optional;

import com.espacogeek.geek.models.JwtTokenModel;
import com.espacogeek.geek.models.UserModel;

/**
 * Interface for JwtTokenService to manage JWT tokens for multi-device support.
 */
public interface JwtTokenService {
    
    /**
     * Save a new JWT token.
     * @param token the JWT token string
     * @param user the user associated with the token
     * @param deviceInfo optional device information
     * @return the saved JwtTokenModel
     */
    JwtTokenModel saveToken(String token, UserModel user, String deviceInfo);
    
    /**
     * Validate if a token exists and is not expired.
     * @param token the JWT token string
     * @return true if valid, false otherwise
     */
    boolean isTokenValid(String token);
    
    /**
     * Find a token by its value.
     * @param token the JWT token string
     * @return Optional containing the token model if found
     */
    Optional<JwtTokenModel> findByToken(String token);
    
    /**
     * Get all tokens for a user.
     * @param userId the user ID
     * @return list of tokens for the user
     */
    List<JwtTokenModel> findByUserId(Integer userId);
    
    /**
     * Delete a specific token.
     * @param token the JWT token string
     */
    void deleteToken(String token);
    
    /**
     * Delete all tokens for a user.
     * @param userId the user ID
     */
    void deleteUserTokens(Integer userId);
    
    /**
     * Clean up expired tokens.
     * @return number of deleted tokens
     */
    int cleanupExpiredTokens();
}
