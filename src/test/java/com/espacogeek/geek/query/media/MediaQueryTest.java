package com.espacogeek.geek.query.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ActiveProfiles;

import com.espacogeek.geek.controllers.MediaController;
import com.espacogeek.geek.data.MediaDataController;
import com.espacogeek.geek.data.api.MediaApi;
import com.espacogeek.geek.models.MediaCategoryModel;
import com.espacogeek.geek.models.MediaModel;
import com.espacogeek.geek.repositories.AlternativeTitlesRepository;
import com.espacogeek.geek.repositories.ExternalReferenceRepository;
import com.espacogeek.geek.repositories.MediaRepository;
import com.espacogeek.geek.repositories.SeasonRepository;
import com.espacogeek.geek.services.MediaCategoryService;
import com.espacogeek.geek.services.MediaService;
import com.espacogeek.geek.services.TypeReferenceService;

@GraphQlTest(MediaController.class)
@ActiveProfiles("test")
class MediaQueryTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private MediaService mediaService;

    @MockitoBean(name = "serieController")
    private MediaDataController serieController;

    @MockitoBean(name = "genericMediaDataController")
    private MediaDataController genericMediaDataController;

    @MockitoBean
    private MediaApi gamesAndVNsAPI;

    @MockitoBean
    private TypeReferenceService typeReferenceService;

    @MockitoBean
    private MediaCategoryService mediaCategoryService;

    @MockitoBean
    private SeasonRepository seasonRepository;

    @MockitoBean
    private AlternativeTitlesRepository alternativeTitlesRepository;

    @MockitoBean
    private ExternalReferenceRepository externalReferenceRepository;

    @MockitoBean
    private MediaRepository mediaRepository;

    @Test
    void media_NotFound_ShouldReturnError() {
        // Given
        when(mediaService.findByIdEager(anyInt())).thenReturn(Optional.empty());

        // When & Then
        graphQlTester.document("""
                query {
                    media(id: 999) {
                        id
                        name
                    }
                }
                """)
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors).isNotEmpty();
                });
    }

    /**
     * Verifies that all 6 {@code @BatchMapping(typeName = "Media")} methods are
     * correctly wired to the {@code Media} GraphQL type. Each batch-loader
     * repository method must be invoked exactly once when the corresponding field
     * is requested. If any {@code @BatchMapping} registration were unmapped (i.e.,
     * still pointing at the non-existent {@code MediaModel} type), Spring for
     * GraphQL would fall back to the default property accessor and the repository
     * method would never be called, causing the {@code verify()} below to fail.
     */
    @Test
    void media_WithAllBatchFields_ShouldInvokeAllSixBatchLoadersConfirmingNoUnmappedRegistrations() {
        // Given
        MediaModel media = new MediaModel();
        media.setId(1);
        media.setName("Breaking Bad");

        when(mediaService.findByIdEager(anyInt())).thenReturn(Optional.of(media));
        when(seasonRepository.findByMediaIn(any())).thenReturn(List.of());
        when(externalReferenceRepository.findAllByMediaIn(any())).thenReturn(List.of());
        when(alternativeTitlesRepository.findByMediaIn(any())).thenReturn(List.of());
        when(mediaRepository.findAllWithGenreByMediaIn(any())).thenReturn(List.of());
        when(mediaRepository.findAllWithCompanyByMediaIn(any())).thenReturn(List.of());
        when(mediaRepository.findAllWithPeopleByMediaIn(any())).thenReturn(List.of());

        // When & Then
        graphQlTester.document("""
                query {
                    media(id: 1) {
                        id
                        name
                        season { id }
                        genre { id }
                        company { id }
                        people { id }
                        externalReference { id }
                        alternativeTitles { id }
                    }
                }
                """)
                .execute()
                .errors().verify()
                .path("media.id").entity(String.class).isEqualTo("1")
                .path("media.name").entity(String.class).isEqualTo("Breaking Bad");

        // Each repository method must have been called via its batch loader.
        // If @BatchMapping(typeName = "Media") were wrong the DataLoader would
        // never fire and these verify() calls would fail.
        verify(seasonRepository).findByMediaIn(any());
        verify(externalReferenceRepository).findAllByMediaIn(any());
        verify(alternativeTitlesRepository).findByMediaIn(any());
        verify(mediaRepository).findAllWithGenreByMediaIn(any());
        verify(mediaRepository).findAllWithCompanyByMediaIn(any());
        verify(mediaRepository).findAllWithPeopleByMediaIn(any());
    }
}
