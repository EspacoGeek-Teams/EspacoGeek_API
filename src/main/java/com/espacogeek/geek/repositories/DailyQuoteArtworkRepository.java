package com.espacogeek.geek.repositories;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.espacogeek.geek.models.DailyQuoteArtworkModel;

@Repository
public interface DailyQuoteArtworkRepository extends JpaRepository<DailyQuoteArtworkModel, Integer> {
    Optional<DailyQuoteArtworkModel> findByDate(LocalDate date);
}
