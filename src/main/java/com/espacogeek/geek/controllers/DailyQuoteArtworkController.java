package com.espacogeek.geek.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.espacogeek.geek.models.DailyQuoteArtworkModel;
import com.espacogeek.geek.services.DailyQuoteArtworkService;
import com.espacogeek.geek.types.QuoteArtwork;

@Controller
public class DailyQuoteArtworkController {

    @Autowired
    private DailyQuoteArtworkService dailyQuoteArtworkService;

    /**
     * Fetches the quote and artwork for today.
     * If one doesn't exist for today, it creates a new one.
     * The same quote and artwork is returned for all requests on the same day.
     *
     * @return A QuoteArtwork object containing the quote, author, and artwork URL for today.
     */
    @QueryMapping(name = "dailyQuoteArtwork")
    public QuoteArtwork getDailyQuoteArtwork() {
        DailyQuoteArtworkModel dailyModel = dailyQuoteArtworkService.getTodayQuoteArtwork();
        
        return new QuoteArtwork(
            dailyModel.getQuote(),
            dailyModel.getAuthor(),
            dailyModel.getUrlArtwork()
        );
    }
}
