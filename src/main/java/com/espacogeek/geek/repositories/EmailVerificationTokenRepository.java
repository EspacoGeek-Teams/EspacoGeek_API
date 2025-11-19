package com.espacogeek.geek.repositories;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.espacogeek.geek.models.EmailVerificationTokenModel;
import com.espacogeek.geek.models.UserModel;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationTokenModel, Integer> {
    
    Optional<EmailVerificationTokenModel> findByToken(String token);
    
    Optional<EmailVerificationTokenModel> findByTokenAndTokenType(String token, String tokenType);
    
    @Modifying
    @Query("DELETE FROM EmailVerificationTokenModel t WHERE t.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
    
    @Modifying
    @Query("DELETE FROM EmailVerificationTokenModel t WHERE t.user = :user AND t.tokenType = :tokenType")
    void deleteByUserAndTokenType(@Param("user") UserModel user, @Param("tokenType") String tokenType);
}
