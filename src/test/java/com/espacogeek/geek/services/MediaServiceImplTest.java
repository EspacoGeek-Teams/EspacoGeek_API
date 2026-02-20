package com.espacogeek.geek.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.espacogeek.geek.models.ExternalReferenceModel;
import com.espacogeek.geek.models.MediaModel;
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

    @InjectMocks
    private MediaServiceImpl mediaService;

    // --- save() tests ---

    @Test
    void save_WithInMemoryExternalReference_ShouldPersist() {
        // Given
        MediaModel media = new MediaModel();
        media.setName("Inception");
        ExternalReferenceModel ref = new ExternalReferenceModel();
        media.setExternalReference(List.of(ref));
        when(mediaRepository.save(media)).thenReturn(media);

        // When
        MediaModel result = mediaService.save(media);

        // Then
        assertThat(result).isEqualTo(media);
        verify(mediaRepository).save(media);
    }

    @Test
    void save_WithNoExternalReferenceAndNoId_ShouldThrowValidationException() {
        // Given
        MediaModel media = new MediaModel();
        media.setName("Unknown");
        media.setExternalReference(null);

        // When & Then
        assertThatThrownBy(() -> mediaService.save(media))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Referência externa obrigatória");

        verify(mediaRepository, never()).save(any());
    }

    @Test
    void save_WithEmptyExternalReferenceListAndNoId_ShouldThrowValidationException() {
        // Given
        MediaModel media = new MediaModel();
        media.setName("Unknown");
        media.setExternalReference(new ArrayList<>());

        // When & Then
        assertThatThrownBy(() -> mediaService.save(media))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Referência externa obrigatória");

        verify(mediaRepository, never()).save(any());
    }

    @Test
    void save_ExistingMediaWithDbRefsAndNoInMemoryRefs_ShouldPersist() {
        // Given
        MediaModel media = new MediaModel();
        media.setId(42);
        media.setName("Breaking Bad");
        media.setExternalReference(null);
        when(externalsRepo.existsByMediaId(42)).thenReturn(true);
        when(mediaRepository.save(media)).thenReturn(media);

        // When
        MediaModel result = mediaService.save(media);

        // Then
        assertThat(result).isEqualTo(media);
        verify(mediaRepository).save(media);
    }

    @Test
    void save_ExistingMediaWithNoDbRefsAndNoInMemoryRefs_ShouldThrowValidationException() {
        // Given
        MediaModel media = new MediaModel();
        media.setId(99);
        media.setName("Orphaned");
        media.setExternalReference(null);
        when(externalsRepo.existsByMediaId(99)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> mediaService.save(media))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Referência externa obrigatória");

        verify(mediaRepository, never()).save(any());
    }

    // --- saveAll() tests ---

    @Test
    void saveAll_AllWithExternalReferences_ShouldSaveAll() {
        // Given
        MediaModel media1 = new MediaModel();
        media1.setName("Movie A");
        media1.setExternalReference(List.of(new ExternalReferenceModel()));

        MediaModel media2 = new MediaModel();
        media2.setName("Movie B");
        media2.setExternalReference(List.of(new ExternalReferenceModel()));

        List<MediaModel> input = List.of(media1, media2);
        when(mediaRepository.saveAll(input)).thenReturn(input);

        // When
        List<MediaModel> result = mediaService.saveAll(input);

        // Then
        assertThat(result).hasSize(2);
        verify(mediaRepository).saveAll(input);
    }

    @Test
    void saveAll_SomeWithoutExternalReferences_ShouldSkipInvalidAndSaveValid() {
        // Given
        MediaModel valid = new MediaModel();
        valid.setName("Valid Movie");
        valid.setExternalReference(List.of(new ExternalReferenceModel()));

        MediaModel invalid = new MediaModel();
        invalid.setName("No Ref Movie");
        invalid.setExternalReference(null);

        List<MediaModel> expected = List.of(valid);
        when(mediaRepository.saveAll(expected)).thenReturn(expected);

        // When
        List<MediaModel> result = mediaService.saveAll(List.of(valid, invalid));

        // Then
        assertThat(result).hasSize(1).containsExactly(valid);
        verify(mediaRepository).saveAll(expected);
    }

    @Test
    void saveAll_AllWithoutExternalReferences_ShouldReturnEmptyListWithoutCallingRepo() {
        // Given
        MediaModel media1 = new MediaModel();
        media1.setName("No Ref A");
        media1.setExternalReference(null);

        MediaModel media2 = new MediaModel();
        media2.setName("No Ref B");
        media2.setExternalReference(new ArrayList<>());

        // When
        List<MediaModel> result = mediaService.saveAll(List.of(media1, media2));

        // Then
        assertThat(result).isEmpty();
        verify(mediaRepository, never()).saveAll(any());
    }

    @Test
    void saveAll_ExistingMediaWithDbRefsAndNoInMemoryRefs_ShouldSave() {
        // Given
        MediaModel media = new MediaModel();
        media.setId(10);
        media.setName("Existing Series");
        media.setExternalReference(null);
        when(externalsRepo.existsByMediaId(10)).thenReturn(true);
        when(mediaRepository.saveAll(any())).thenReturn(List.of(media));

        // When
        List<MediaModel> result = mediaService.saveAll(List.of(media));

        // Then
        assertThat(result).hasSize(1);
        verify(mediaRepository).saveAll(any());
    }

    @Test
    void saveAll_EmptyList_ShouldReturnEmptyList() {
        // When
        List<MediaModel> result = mediaService.saveAll(List.of());

        // Then
        assertThat(result).isEmpty();
        verify(mediaRepository, never()).saveAll(any());
    }
}
