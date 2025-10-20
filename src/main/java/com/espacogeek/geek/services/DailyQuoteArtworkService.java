package com.espacogeek.geek.services;

import java.util.Date;
import java.util.Optional;

import com.espacogeek.geek.models.DailyQuoteArtworkModel;

public interface DailyQuoteArtworkService {
    /**
     * Finds the daily quote and artwork for a specific date.
     *
     * @param date The date to search for
     * @return Optional containing the DailyQuoteArtworkModel if found
     */
    Optional<DailyQuoteArtworkModel> findByDate(Date date);

    /**
     * Saves a new daily quote and artwork.
     *
     * @param dailyQuoteArtwork The model to save
     * @return The saved DailyQuoteArtworkModel
     */
    DailyQuoteArtworkModel save(DailyQuoteArtworkModel dailyQuoteArtwork);

    /**
     * Gets or creates the daily quote and artwork for today.
     * If one doesn't exist for today, fetches a new random quote and artwork.
     *
     * @return The DailyQuoteArtworkModel for today
     */
    DailyQuoteArtworkModel getTodayQuoteArtwork();
}
