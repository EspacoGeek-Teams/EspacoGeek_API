package com.espacogeek.geek.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.espacogeek.geek.models.JwtTokenModel;

@Repository
public interface JwtTokenRepository extends JpaRepository<JwtTokenModel, Integer> {
    
    /**
     * Find a token by its value.
     * @param token the JWT token string
     * @return Optional containing the token model if found
     */
    Optional<JwtTokenModel> findByToken(String token);
    
    /**
     * Find all tokens for a specific user.
     * @param userId the user ID
     * @return list of tokens for the user
     */
    @Query("SELECT t FROM JwtTokenModel t WHERE t.user.id = ?1")
    List<JwtTokenModel> findByUserId(Integer userId);
    
    /**
     * Delete expired tokens.
     * @param now current timestamp
     * @return number of deleted tokens
     */
    @Modifying
    @Query("DELETE FROM JwtTokenModel t WHERE t.expiresAt < ?1")
    int deleteExpiredTokens(LocalDateTime now);
    
    /**
     * Delete all tokens for a specific user.
     * @param userId the user ID
     */
    @Modifying
    @Query("DELETE FROM JwtTokenModel t WHERE t.user.id = ?1")
    void deleteByUserId(Integer userId);
}
