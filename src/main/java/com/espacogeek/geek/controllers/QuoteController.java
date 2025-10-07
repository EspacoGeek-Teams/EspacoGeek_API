package com.espacogeek.geek.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.espacogeek.geek.data.api.QuoteApi;
import com.espacogeek.geek.exception.GenericException;
import com.espacogeek.geek.services.MediaService;
import com.espacogeek.geek.types.QuoteArtwork;

@Controller
public class QuoteController {
    @Autowired
    private QuoteApi quoteApi;
    @Autowired
    private MediaService mediaService;

    /**
     * Fetches a random quote along with a random artwork URL.
     *
     * @return A QuoteArtwork object containing the quote, author, and artwork URL.
     * @throws GenericException if no artwork is found.
     */
    @QueryMapping(name = "quote")
    public QuoteArtwork getQuoteAndRandomArtwork() {
        var quote = quoteApi.getRandomQuote();
        String artwork = mediaService.randomArtwork().orElseThrow(() -> new GenericException("Artwork not found"));

        return new QuoteArtwork(quote.getQuote(), quote.getAuthor(), artwork);
    }
}
