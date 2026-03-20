package com.espacogeek.geek.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.HashSet;
import java.util.List;

import jakarta.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.espacogeek.geek.data.MediaDataController;
import com.espacogeek.geek.models.ExternalReferenceModel;
import com.espacogeek.geek.models.MediaCategoryModel;
import com.espacogeek.geek.models.MediaModel;
import com.espacogeek.geek.models.TypeReferenceModel;
import com.espacogeek.geek.repositories.ExternalReferenceRepository;
import com.espacogeek.geek.repositories.MediaRepository;
import com.espacogeek.geek.services.impl.MediaServiceImpl;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class MediaServiceImplTest {

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private ExternalReferenceRepository externalsRepo;

    @Mock
    private MediaCategoryService mediaCategoryService;

    @Mock
    private TypeReferenceService typeReferenceService;

    @Mock(name = "serieController")
    private com.espacogeek.geek.data.MediaDataController serieController;

    @Mock(name = "genericMediaDataController")
    private com.espacogeek.geek.data.MediaDataController genericMediaDataController;

    @Mock(name = "gamesAndVNsAPI")
    private com.espacogeek.geek.data.api.MediaApi gamesAndVNsAPI;

    @Mock(name = "movieAPI")
    private com.espacogeek.geek.data.api.MediaApi movieAPI;

    private MediaServiceImpl mediaService;

    @BeforeEach
    void setUp() {
        mediaService = new MediaServiceImpl(
                mediaRepository,
                externalsRepo,
                mediaCategoryService,
                serieController,
                genericMediaDataController,
                typeReferenceService,
                gamesAndVNsAPI,
                movieAPI);
    }

    @Test
    void save_WithInMemoryExternalReference_ShouldPersist() {
        MediaModel media = new MediaModel();
        media.setName("Inception");
        ExternalReferenceModel ref = new ExternalReferenceModel();
        media.setExternalReference(new HashSet<>(List.of(ref)));
        when(mediaRepository.save(media)).thenReturn(media);

        MediaModel result = mediaService.save(media);

        assertThat(result).isEqualTo(media);
        verify(mediaRepository).save(media);
    }

    @Test
    void save_WithNoExternalReferenceAndNoId_ShouldThrowValidationException() {
        MediaModel media = new MediaModel();
        media.setName("Unknown");
        media.setExternalReference(null);

        assertThatThrownBy(() -> mediaService.save(media))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Referência externa obrigatória");

        verify(mediaRepository, never()).save(any());
    }

    @Test
    void save_WithEmptyExternalReferenceListAndNoId_ShouldThrowValidationException() {
        MediaModel media = new MediaModel();
        media.setName("Unknown");
        media.setExternalReference(new HashSet<>());

        assertThatThrownBy(() -> mediaService.save(media))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Referência externa obrigatória");

        verify(mediaRepository, never()).save(any());
    }

    @Test
    void save_ExistingMediaWithDbRefsAndNoInMemoryRefs_ShouldPersist() {
        MediaModel media = new MediaModel();
        media.setId(42);
        media.setName("Breaking Bad");
        media.setExternalReference(null);
        when(externalsRepo.existsByMediaId(42)).thenReturn(true);
        when(mediaRepository.save(media)).thenReturn(media);

        MediaModel result = mediaService.save(media);

        assertThat(result).isEqualTo(media);
        verify(mediaRepository).save(media);
    }

    @Test
    void save_ExistingMediaWithNoDbRefsAndNoInMemoryRefs_ShouldThrowValidationException() {
        MediaModel media = new MediaModel();
        media.setId(99);
        media.setName("Orphaned");
        media.setExternalReference(null);
        when(externalsRepo.existsByMediaId(99)).thenReturn(false);

        assertThatThrownBy(() -> mediaService.save(media))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Referência externa obrigatória");

        verify(mediaRepository, never()).save(any());
    }

    @Test
    void saveAll_AllWithExternalReferences_ShouldSaveAll() {
        MediaModel media1 = new MediaModel();
        media1.setName("Movie A");
        media1.setExternalReference(new HashSet<>(List.of(new ExternalReferenceModel())));

        MediaModel media2 = new MediaModel();
        media2.setName("Movie B");
        media2.setExternalReference(new HashSet<>(List.of(new ExternalReferenceModel())));

        List<MediaModel> input = List.of(media1, media2);
        when(mediaRepository.saveAll(input)).thenReturn(input);

        List<MediaModel> result = mediaService.saveAll(input);

        assertThat(result).hasSize(2);
        verify(mediaRepository).saveAll(input);
    }

    @Test
    void saveAll_SomeWithoutExternalReferences_ShouldSkipInvalidAndSaveValid() {
        MediaModel valid = new MediaModel();
        valid.setName("Valid Movie");
        valid.setExternalReference(new HashSet<>(List.of(new ExternalReferenceModel())));

        MediaModel invalid = new MediaModel();
        invalid.setName("No Ref Movie");
        invalid.setExternalReference(null);

        List<MediaModel> expected = List.of(valid);
        when(mediaRepository.saveAll(expected)).thenReturn(expected);

        List<MediaModel> result = mediaService.saveAll(List.of(valid, invalid));

        assertThat(result).hasSize(1).containsExactly(valid);
        verify(mediaRepository).saveAll(expected);
    }

    @Test
    void saveAll_AllWithoutExternalReferences_ShouldReturnEmptyListWithoutCallingRepo() {
        MediaModel media1 = new MediaModel();
        media1.setName("No Ref A");
        media1.setExternalReference(null);

        MediaModel media2 = new MediaModel();
        media2.setName("No Ref B");
        media2.setExternalReference(new HashSet<>());

        List<MediaModel> result = mediaService.saveAll(List.of(media1, media2));

        assertThat(result).isEmpty();
        verify(mediaRepository, never()).saveAll(any());
    }

    @Test
    void saveAll_ExistingMediaWithDbRefsAndNoInMemoryRefs_ShouldSave() {
        MediaModel media = new MediaModel();
        media.setId(10);
        media.setName("Existing Series");
        media.setExternalReference(null);
        when(externalsRepo.existsByMediaId(10)).thenReturn(true);
        when(mediaRepository.saveAll(any())).thenReturn(List.of(media));

        List<MediaModel> result = mediaService.saveAll(List.of(media));

        assertThat(result).hasSize(1);
        verify(mediaRepository).saveAll(any());
    }

    @Test
    void saveAll_EmptyList_ShouldReturnEmptyList() {
        List<MediaModel> result = mediaService.saveAll(List.of());

        assertThat(result).isEmpty();
        verify(mediaRepository, never()).saveAll(any());
    }

    @Test
    void findGameByIdOrName_WhenDatabaseIsEmpty_ShouldFallbackToExternalSearchAndReturnResults() {
        MediaCategoryModel gameCategory = new MediaCategoryModel();
        gameCategory.setId(MediaDataController.MediaType.GAME.getId());

        TypeReferenceModel igdbReference = new TypeReferenceModel();
        igdbReference.setId(MediaDataController.ExternalReferenceType.IGDB.getId());

        MediaModel externalGame = new MediaModel();
        externalGame.setId(101);
        externalGame.setName("Chrono Trigger");
        externalGame.setMediaCategory(gameCategory);

        when(mediaCategoryService.findById(MediaDataController.MediaType.GAME.getId())).thenReturn(java.util.Optional.of(gameCategory));
        when(mediaRepository.findMediaByNameOrAlternativeTitleAndMediaCategory(eq("chrono"), eq("chrono"), eq(gameCategory.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
        when(typeReferenceService.findById(MediaDataController.ExternalReferenceType.IGDB.getId())).thenReturn(java.util.Optional.of(igdbReference));
        when(genericMediaDataController.searchMedia(anyString(), eq(gamesAndVNsAPI), eq(igdbReference), eq(gameCategory)))
                .thenReturn(List.of(externalGame));

        var result = mediaService.findGameByIdOrName(null, "chrono", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getName()).isEqualTo("Chrono Trigger");
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(genericMediaDataController).searchMedia("chrono", gamesAndVNsAPI, igdbReference, gameCategory);
    }

    @Test
    void findVisualNovelByIdOrName_WhenLocalResultIsStale_ShouldRefreshBeforeReturning() {
        MediaCategoryModel vnCategory = new MediaCategoryModel();
        vnCategory.setId(MediaDataController.MediaType.VN.getId());

        TypeReferenceModel igdbReference = new TypeReferenceModel();
        igdbReference.setId(MediaDataController.ExternalReferenceType.IGDB.getId());

        ExternalReferenceModel reference = new ExternalReferenceModel();
        reference.setReference("202");
        reference.setTypeReference(igdbReference);

        MediaModel staleVisualNovel = new MediaModel();
        staleVisualNovel.setId(202);
        staleVisualNovel.setName("Old VN");
        staleVisualNovel.setMediaCategory(vnCategory);
        staleVisualNovel.setExternalReference(new HashSet<>(List.of(reference)));
        staleVisualNovel.setUpdateAt(new Date(0));

        MediaModel refreshedVisualNovel = new MediaModel();
        refreshedVisualNovel.setId(202);
        refreshedVisualNovel.setName("Refreshed VN");
        refreshedVisualNovel.setMediaCategory(vnCategory);
        refreshedVisualNovel.setExternalReference(new HashSet<>(List.of(reference)));

        when(mediaCategoryService.findById(MediaDataController.MediaType.VN.getId())).thenReturn(java.util.Optional.of(vnCategory));
        when(mediaRepository.findMediaByNameOrAlternativeTitleAndMediaCategory(eq("steins"), eq("steins"), eq(vnCategory.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(staleVisualNovel), PageRequest.of(0, 10), 1));
        when(typeReferenceService.findById(MediaDataController.ExternalReferenceType.IGDB.getId())).thenReturn(java.util.Optional.of(igdbReference));
        when(genericMediaDataController.updateAllInformation(eq(staleVisualNovel), isNull(), eq(igdbReference), eq(gamesAndVNsAPI)))
                .thenReturn(refreshedVisualNovel);

        var result = mediaService.findVisualNovelByIdOrName(null, "steins", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getName()).isEqualTo("Refreshed VN");
        verify(genericMediaDataController).updateAllInformation(staleVisualNovel, null, igdbReference, gamesAndVNsAPI);
        verify(genericMediaDataController, never()).searchMedia(any(), any(), any(), any());
    }
}
