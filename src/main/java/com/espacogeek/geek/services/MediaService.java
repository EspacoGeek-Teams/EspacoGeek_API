package com.espacogeek.geek.services;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

import com.espacogeek.geek.models.ExternalReferenceModel;
import com.espacogeek.geek.models.MediaModel;
import com.espacogeek.geek.models.TypeReferenceModel;
import com.espacogeek.geek.types.MediaPage;

/**
 * Interface for the MediaService, which provides methods for managing MediaModel objects.
 */
public interface MediaService {
    MediaPage findSerieByIdOrName(Integer id, String name, Pageable pageable);

    MediaPage findSerieByIdOrName(Integer id, String name, Map<String, List<String>> requestedFields, Pageable pageable);

    MediaPage findGameByIdOrName(Integer id, String name, Pageable pageable);

    MediaPage findMovieByIdOrName(Integer id, String name, Map<String, List<String>> requestedFields, Pageable pageable);

    MediaModel save(MediaModel media);

    List<MediaModel> saveAll(List<MediaModel> medias);

    Optional<MediaModel> findByReferenceAndTypeReference(ExternalReferenceModel reference, TypeReferenceModel typeReferenceModel);

    /**
     * Find any media by ID (PK) provided with eager loading.
     * @param idMedia the ID (PK) of the media.
     * @return return a Optional Media.
     */
    Optional<MediaModel> findByIdEager(Integer id);

    /**
     * Returns a random artwork URL if available.
     *
     * @return An Optional containing a random artwork URL, or an empty Optional if no artwork is found.
     */
    Optional<String> randomArtwork();

    MediaPage findAnimeByIdOrName(Integer id, String name, Pageable pageable);

    MediaPage findMovieByIdOrName(Integer id, String name, Pageable pageable);

    MediaPage findVisualNovelByIdOrName(Integer id, String name, Pageable pageable);
}
