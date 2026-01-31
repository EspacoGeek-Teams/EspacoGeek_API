package com.espacogeek.geek.services;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

import com.espacogeek.geek.models.ExternalReferenceModel;
import com.espacogeek.geek.models.MediaModel;
import com.espacogeek.geek.models.TypeReferenceModel;

/**
 * Interface for the MediaService, which provides methods for managing MediaModel objects.
 */
public interface MediaService {
    /**
     * Finds Series (MediaModel) objects by their ID or name.
     *
     * @param id   The ID of the Series (MediaModel) object to find.
     * @param name The name of the Series (MediaModel) object to find.
     * @return A list of Series (MediaModel) objects that match the provided ID or name.
     */
    Page<MediaModel> findSerieByIdOrName(Integer id, String name, Pageable pageable);

    /**
     * Finds Series (MediaModel) objects by their ID or name.
     *
     * @param id   The ID of the Series (MediaModel) object to find.
     * @param name The name of the Series (MediaModel) object to find.
     * @param requestedFields The fields to include in the response.
     * @return A list of Series (MediaModel) objects that match the provided ID or name.
     */
    Page<MediaModel> findSerieByIdOrName(Integer id, String name, Map<String, List<String>> requestedFields, Pageable pageable);

    /**
     * Finds Game (MediaModel) objects by their ID or name.
     *
     * @param id   The ID of the Game (MediaModel) object to find.
     * @param name The name of the Game (MediaModel) object to find.
     * @return A list of Game (MediaModel) objects that match the provided ID or name.
     */
    Page<MediaModel> findGameByIdOrName(Integer id, String name, Pageable pageable);

    Page<MediaModel> findMovieByIdOrName(Integer id, String name, Map<String, List<String>> requestedFields, Pageable pageable);

    /**
     * Saves a MediaModel object to the database.
     *
     * @param media The MediaModel object to save.
     * @return The saved MediaModel object.
     */
    MediaModel save(MediaModel media);

    /**
     * Saves a list of MediaModel objects to the database.
     *
     * @param medias The list of MediaModel objects to save.
     * @return The list of saved MediaModel objects.
     */
    List<MediaModel> saveAll(List<MediaModel> medias);

    /**
     * Find any media by ID (PK) provided.
     * @param idMedia the ID (PK) of the media.
     * @return return a Optional Media.
     */
    Optional<MediaModel> findById(Integer idMedia);

    /**
     * Find media by <code>ExternalReference</code> and <code>TypeReference</code>.
     *
     * @param reference
     * @param typeReferenceModel
     * @return a Optional MediaModel
     */
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

    /**
     * Finds Anime (MediaModel) objects by their ID or name.
     *
     * @param id   The ID of the Anime (MediaModel) object to find.
     * @param name The name of the Anime (MediaModel) object to find.
     * @return A list of Anime (MediaModel) objects that match the provided ID or name.
     */
    Page<MediaModel> findAnimeByIdOrName(Integer id, String name, Pageable pageable);
}
