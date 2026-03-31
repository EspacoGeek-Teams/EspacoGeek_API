package com.espacogeek.geek.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import com.espacogeek.geek.data.MediaDataController;
import com.espacogeek.geek.data.api.MediaApi;
import com.espacogeek.geek.data.impl.GenericMediaDataControllerImpl;
import com.espacogeek.geek.models.AlternativeTitleModel;
import com.espacogeek.geek.models.ExternalReferenceModel;
import com.espacogeek.geek.models.CategoryType;
import com.espacogeek.geek.models.MediaCategoryModel;
import com.espacogeek.geek.models.MediaModel;
import com.espacogeek.geek.models.TypeReferenceModel;
import com.espacogeek.geek.repositories.AlternativeTitlesRepository;
import com.espacogeek.geek.repositories.ExternalReferenceRepository;
import com.espacogeek.geek.repositories.GenreRepository;
import com.espacogeek.geek.repositories.MediaCategoryRepository;
import com.espacogeek.geek.repositories.MediaRepository;
import com.espacogeek.geek.repositories.TypeReferenceRepository;
import com.espacogeek.geek.services.impl.AlternativeTitlesServiceImpl;
import com.espacogeek.geek.services.impl.ExternalReferenceServiceImpl;
import com.espacogeek.geek.services.impl.GenreServiceImpl;
import com.espacogeek.geek.services.impl.MediaServiceImpl;
import com.espacogeek.geek.types.MediaPage;
import com.espacogeek.geek.utils.MediaLazyLoader;

@DataJpaTest
@ActiveProfiles("test")
class MediaServicePersistenceIntegrationTest {

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private ExternalReferenceRepository externalReferenceRepository;

    @Autowired
    private AlternativeTitlesRepository alternativeTitlesRepository;

    @Autowired
    private MediaCategoryRepository mediaCategoryRepository;

    @Autowired
    private TypeReferenceRepository typeReferenceRepository;

    @Autowired
    private GenreRepository genreRepository;

    private MediaService mediaService;
    private MediaApi gamesAndVNsAPI;
    private MediaCategoryModel gameCategory;
    private MediaCategoryModel vnCategory;
    private TypeReferenceModel igdbTypeReference;

    @BeforeEach
    void setUp() {
        gameCategory = mediaCategoryRepository.save(new MediaCategoryModel(null, CategoryType.GAME, null));
        vnCategory = mediaCategoryRepository.save(new MediaCategoryModel(null, CategoryType.VISUAL_NOVEL, null));
        igdbTypeReference = typeReferenceRepository.save(new TypeReferenceModel(null, "IGDB", null));

        MediaCategoryService mediaCategoryService = mock(MediaCategoryService.class);
        when(mediaCategoryService.findById(MediaDataController.MediaType.GAME.getId())).thenReturn(Optional.of(gameCategory));
        when(mediaCategoryService.findById(MediaDataController.MediaType.VN.getId())).thenReturn(Optional.of(vnCategory));

        TypeReferenceService typeReferenceService = mock(TypeReferenceService.class);
        when(typeReferenceService.findById(MediaDataController.ExternalReferenceType.IGDB.getId())).thenReturn(Optional.of(igdbTypeReference));

        AlternativeTitlesServiceImpl alternativeTitlesService = new AlternativeTitlesServiceImpl();
        ReflectionTestUtils.setField(alternativeTitlesService, "alternativeTitlesRepository", alternativeTitlesRepository);

        ExternalReferenceServiceImpl externalReferenceService = new ExternalReferenceServiceImpl();
        ReflectionTestUtils.setField(externalReferenceService, "externalReferenceRepository", externalReferenceRepository);

        GenreServiceImpl genreService = new GenreServiceImpl();
        ReflectionTestUtils.setField(genreService, "genreRepository", genreRepository);

        SeasonService seasonService = mock(SeasonService.class);
        when(seasonService.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(seasonService.findAll(any())).thenReturn(java.util.List.of());

        gamesAndVNsAPI = mock(MediaApi.class);
        MediaApi movieAPI = mock(MediaApi.class);
        MediaApi tvSeriesApi = mock(MediaApi.class);
        MediaDataController serieController = mock(MediaDataController.class);

        MediaLazyLoader mediaLazyLoader = new MediaLazyLoader(
                externalReferenceService,
                alternativeTitlesService,
                genreService,
                seasonService);

        GenericMediaDataControllerImpl genericMediaDataController = new GenericMediaDataControllerImpl(
                null,
                genreService,
                alternativeTitlesService,
                externalReferenceService,
                seasonService,
                mediaLazyLoader);

        mediaService = new MediaServiceImpl(
                mediaRepository,
                externalReferenceRepository,
                mediaCategoryService,
                serieController,
                genericMediaDataController,
                typeReferenceService,
                gamesAndVNsAPI,
                movieAPI,
                tvSeriesApi,
                mediaLazyLoader);

        ReflectionTestUtils.setField(genericMediaDataController, "mediaService", mediaService);
    }

    @Test
    void gameSearch_WhenMissingLocally_ShouldPersistFetchedMediaAndReuseDatabaseOnSecondSearch() {
        MediaModel externalGame = externalSearchResult("Chrono Trigger", "chrono-trigger-igdb", gameCategory);
                when(gamesAndVNsAPI.doSearch(eq("Chrono"), any(MediaCategoryModel.class))).thenReturn(List.of(externalGame));

                MediaPage firstResult = mediaService.findGameByIdOrName(null, "Chrono", PageRequest.of(0, 10));

        assertThat(firstResult.getContent()).hasSize(1);
        assertThat(firstResult.getContent().getFirst().getName()).isEqualTo("Chrono Trigger");
        assertThat(mediaRepository.count()).isEqualTo(1);

        MediaModel persistedGame = externalReferenceRepository
                .findByReferenceAndTypeReference("chrono-trigger-igdb", igdbTypeReference)
                .orElseThrow()
                .getMedia();

        assertThat(persistedGame.getId()).isNotNull();
        assertThat(externalReferenceRepository.findByReferenceAndTypeReference("chrono-trigger-igdb", igdbTypeReference)).isPresent();
        assertThat(alternativeTitlesRepository.findByMediaIn(List.of(persistedGame)))
                .extracting(AlternativeTitleModel::getName)
                .contains("Chrono Trigger Alternative");

        MediaPage secondResult = mediaService.findGameByIdOrName(null, "Chrono", PageRequest.of(0, 10));

        assertThat(secondResult.getContent()).hasSize(1);
        assertThat(secondResult.getContent().getFirst().getId()).isEqualTo(firstResult.getContent().getFirst().getId());
                verify(gamesAndVNsAPI, times(1)).doSearch(eq("Chrono"), any(MediaCategoryModel.class));
    }

    @Test
    void visualNovelSearch_WhenMissingLocally_ShouldPersistFetchedMediaAndReuseDatabaseOnSecondSearch() {
        MediaModel externalVn = externalSearchResult("Steins;Gate", "steins-gate-igdb", vnCategory);
                when(gamesAndVNsAPI.doSearch(eq("Steins"), any(MediaCategoryModel.class))).thenReturn(List.of(externalVn));

                MediaPage firstResult = mediaService.findVisualNovelByIdOrName(null, "Steins", PageRequest.of(0, 10));

        assertThat(firstResult.getContent()).hasSize(1);
        assertThat(firstResult.getContent().getFirst().getName()).isEqualTo("Steins;Gate");
        assertThat(mediaRepository.count()).isEqualTo(1);

        MediaModel persistedVn = externalReferenceRepository
                .findByReferenceAndTypeReference("steins-gate-igdb", igdbTypeReference)
                .orElseThrow()
                .getMedia();

        assertThat(persistedVn.getId()).isNotNull();
        assertThat(externalReferenceRepository.findByReferenceAndTypeReference("steins-gate-igdb", igdbTypeReference)).isPresent();
        assertThat(alternativeTitlesRepository.findByMediaIn(List.of(persistedVn)))
                .extracting(AlternativeTitleModel::getName)
                .contains("Steins;Gate Alternative");

        MediaPage secondResult = mediaService.findVisualNovelByIdOrName(null, "Steins", PageRequest.of(0, 10));

        assertThat(secondResult.getContent()).hasSize(1);
        assertThat(secondResult.getContent().getFirst().getId()).isEqualTo(firstResult.getContent().getFirst().getId());
                verify(gamesAndVNsAPI, times(1)).doSearch(eq("Steins"), any(MediaCategoryModel.class));
    }

    private MediaModel externalSearchResult(String name, String externalReferenceValue, MediaCategoryModel mediaCategory) {
        MediaModel media = new MediaModel();
        media.setName(name);
        media.setAbout(name + " description");
        media.setCover("https://example.com/" + externalReferenceValue + ".png");
        media.setBanner("https://example.com/" + externalReferenceValue + "-banner.png");
        media.setMediaCategory(mediaCategory);

        AlternativeTitleModel alternativeTitle = new AlternativeTitleModel(null, name + " Alternative", media);
        media.setAlternativeTitles(new LinkedHashSet<>(List.of(alternativeTitle)));

        ExternalReferenceModel externalReference = new ExternalReferenceModel(null, externalReferenceValue, media, igdbTypeReference);
        media.setExternalReference(new LinkedHashSet<>(List.of(externalReference)));

        return media;
    }
}
