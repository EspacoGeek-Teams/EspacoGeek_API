package com.espacogeek.geek.query.quote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ActiveProfiles;

import com.espacogeek.geek.controllers.DailyQuoteArtworkController;
import com.espacogeek.geek.models.DailyQuoteArtworkModel;
import com.espacogeek.geek.services.DailyQuoteArtworkService;
import com.espacogeek.geek.types.QuoteArtwork;

import java.util.Date;

@GraphQlTest(DailyQuoteArtworkController.class)
@ActiveProfiles("test")
class DailyQuoteArtworkQueryTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockBean
    private DailyQuoteArtworkService dailyQuoteArtworkService;

    @Test
    void dailyQuoteArtwork_ShouldReturnTodaysQuoteWithArtwork() {
        // Given
        DailyQuoteArtworkModel dailyModel = new DailyQuoteArtworkModel();
        dailyModel.setId(1);
        dailyModel.setQuote("The only way to do great work is to love what you do.");
        dailyModel.setAuthor("Steve Jobs");
        dailyModel.setUrlArtwork("https://example.com/artwork.jpg");
        dailyModel.setDate(new Date());
        dailyModel.setCreatedAt(new Date());

        when(dailyQuoteArtworkService.getTodayQuoteArtwork()).thenReturn(dailyModel);

        // When & Then
        graphQlTester.document("""
                query {
                    dailyQuoteArtwork {
                        quote
                        author
                        urlArtwork
                    }
                }
                """)
                .execute()
                .path("dailyQuoteArtwork")
                .entity(QuoteArtwork.class)
                .satisfies(result -> {
                    assertThat(result.getQuote()).isEqualTo("The only way to do great work is to love what you do.");
                    assertThat(result.getAuthor()).isEqualTo("Steve Jobs");
                    assertThat(result.getUrlArtwork()).isEqualTo("https://example.com/artwork.jpg");
                });
    }

    @Test
    void dailyQuoteArtwork_MultipleCalls_ShouldReturnSameQuoteForToday() {
        // Given
        DailyQuoteArtworkModel dailyModel = new DailyQuoteArtworkModel();
        dailyModel.setId(1);
        dailyModel.setQuote("Test quote for today");
        dailyModel.setAuthor("Test Author");
        dailyModel.setUrlArtwork("https://example.com/test-artwork.jpg");
        dailyModel.setDate(new Date());
        dailyModel.setCreatedAt(new Date());

        when(dailyQuoteArtworkService.getTodayQuoteArtwork()).thenReturn(dailyModel);

        // When & Then - First call
        QuoteArtwork firstResult = graphQlTester.document("""
                query {
                    dailyQuoteArtwork {
                        quote
                        author
                        urlArtwork
                    }
                }
                """)
                .execute()
                .path("dailyQuoteArtwork")
                .entity(QuoteArtwork.class)
                .get();

        // When & Then - Second call
        QuoteArtwork secondResult = graphQlTester.document("""
                query {
                    dailyQuoteArtwork {
                        quote
                        author
                        urlArtwork
                    }
                }
                """)
                .execute()
                .path("dailyQuoteArtwork")
                .entity(QuoteArtwork.class)
                .get();

        // Both should return the same quote
        assertThat(firstResult.getQuote()).isEqualTo(secondResult.getQuote());
        assertThat(firstResult.getAuthor()).isEqualTo(secondResult.getAuthor());
        assertThat(firstResult.getUrlArtwork()).isEqualTo(secondResult.getUrlArtwork());
    }
}
