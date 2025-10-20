package com.espacogeek.geek.services.impl;

import java.time.LocalDate;
import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.espacogeek.geek.data.api.QuoteApi;
import com.espacogeek.geek.exception.GenericException;
import com.espacogeek.geek.models.DailyQuoteArtworkModel;
import com.espacogeek.geek.models.QuoteModel;
import com.espacogeek.geek.repositories.DailyQuoteArtworkRepository;
import com.espacogeek.geek.services.DailyQuoteArtworkService;
import com.espacogeek.geek.services.MediaService;

@Service
public class DailyQuoteArtworkServiceImpl implements DailyQuoteArtworkService {

    @Autowired
    private DailyQuoteArtworkRepository dailyQuoteArtworkRepository;

    @Autowired
    private QuoteApi quoteApi;

    @Autowired
    private MediaService mediaService;

    /**
     * @see DailyQuoteArtworkService#findByDate(LocalDate)
     */
    @Override
    public Optional<DailyQuoteArtworkModel> findByDate(Date date) {
        return dailyQuoteArtworkRepository.findByDate(date);
    }

    /**
     * @see DailyQuoteArtworkService#save(DailyQuoteArtworkModel)
     */
    @Override
    public DailyQuoteArtworkModel save(DailyQuoteArtworkModel dailyQuoteArtwork) {
        return dailyQuoteArtworkRepository.save(dailyQuoteArtwork);
    }

    /**
     * @see DailyQuoteArtworkService#getTodayQuoteArtwork()
     */
    @Override
    public DailyQuoteArtworkModel getTodayQuoteArtwork() {
        Date today = new Date();

        // Try to find existing quote for today
        Optional<DailyQuoteArtworkModel> existing = findByDate(today);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Create new quote for today
        QuoteModel quote = quoteApi.getRandomQuote();
        String artwork = mediaService.randomArtwork().orElseThrow(() -> new GenericException("Artwork not found"));

        DailyQuoteArtworkModel dailyQuoteArtwork = new DailyQuoteArtworkModel();
        dailyQuoteArtwork.setQuote(quote.getQuote());
        dailyQuoteArtwork.setAuthor(quote.getAuthor());
        dailyQuoteArtwork.setUrlArtwork(artwork);
        dailyQuoteArtwork.setDate(today);

        return save(dailyQuoteArtwork);
    }
}
