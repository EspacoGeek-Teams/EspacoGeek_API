package com.espacogeek.geek.query.quote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ActiveProfiles;

import com.espacogeek.geek.controllers.QuoteController;
import com.espacogeek.geek.data.api.QuoteApi;
import com.espacogeek.geek.models.QuoteModel;
import com.espacogeek.geek.services.MediaService;
import com.espacogeek.geek.types.QuoteArtwork;

@GraphQlTest(QuoteController.class)
@ActiveProfiles("test")
class QuoteQueryTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private QuoteApi quoteApi;

    @MockitoBean
    private MediaService mediaService;

    @Test
    void quote_ShouldReturnQuoteWithArtwork() {
        // Given
        QuoteModel quote = new QuoteModel("Life is like riding a bicycle.", "Albert Einstein");
        String artworkUrl = "https://example.com/artwork.jpg";

        when(quoteApi.getRandomQuote()).thenReturn(quote);
        when(mediaService.randomArtwork()).thenReturn(Optional.of(artworkUrl));

        // When & Then
        graphQlTester.document("""
                query {
                    quote {
                        quote
                        author
                        urlArtwork
                    }
                }
                """)
                .execute()
                .path("quote")
                .entity(QuoteArtwork.class)
                .satisfies(result -> {
                    assertThat(result.getQuote()).isEqualTo("Life is like riding a bicycle.");
                    assertThat(result.getAuthor()).isEqualTo("Albert Einstein");
                    assertThat(result.getUrlArtwork()).isEqualTo(artworkUrl);
                });
    }

    @Test
    void quote_NoArtworkAvailable_ShouldReturnError() {
        // Given
        QuoteModel quote = new QuoteModel("Test quote", "Test author");

        when(quoteApi.getRandomQuote()).thenReturn(quote);
        when(mediaService.randomArtwork()).thenReturn(Optional.empty());

        // When & Then
        graphQlTester.document("""
                query {
                    quote {
                        quote
                        author
                        urlArtwork
                    }
                }
                """)
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors).isNotEmpty();
                });
    }
}
