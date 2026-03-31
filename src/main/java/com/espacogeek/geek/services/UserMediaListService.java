package com.espacogeek.geek.services;

import java.util.List;

import com.espacogeek.geek.models.CategoryType;
import com.espacogeek.geek.models.UserMediaListModel;
import com.espacogeek.geek.types.UpdateUserMediaInput;

/**
 * Interface for the UserMediaListService, which provides methods for managing
 * the user's personal media library.
 */
public interface UserMediaListService {

    /**
     * Retrieves all library entries for the given user, with optional filters.
     *
     * @param userId       the ID of the authenticated user
     * @param status       optional user tracking status string filter (e.g. "watching", "completed")
     * @param statusId     optional media production status ID filter (MediaStatusModel.id)
     * @param categoryId   optional media category ID filter
     * @param categoryName optional media category name filter (e.g. {@link CategoryType#ANIME})
     * @param genreId      optional genre ID filter
     * @param genreName    optional genre name filter
     * @param mediaId      optional media ID filter
     * @param mediaName    optional media name filter (partial match)
     * @param altTitle     optional alternative title filter (partial match)
     * @return a list of matching {@link UserMediaListModel} entries
     */
    List<UserMediaListModel> findByUserIdWithFilters(
            Integer userId,
            String status,
            Integer statusId,
            Integer categoryId,
            CategoryType categoryName,
            Integer genreId,
            String genreName,
            Integer mediaId,
            String mediaName,
            String altTitle);

    /**
     * Adds a media item to the user's library with default status
     * ({@link com.espacogeek.geek.models.StatusType#PLANNING}) and progress 0.
     * Throws {@link com.espacogeek.geek.exception.MediaAlreadyInLibraryException} if
     * the entry already exists.
     *
     * @param userId  the ID of the authenticated user
     * @param mediaId the ID of the media to add
     * @return the newly created {@link UserMediaListModel} entry
     */
    UserMediaListModel addMedia(Integer userId, Integer mediaId);

    /**
     * Creates or updates the user's library entry for the specified media (upsert).
     * If an entry for the given user and {@code input.mediaId} already exists, it is
     * updated with only the non-null fields from {@code input}. If no entry exists, a
     * new one is created with the provided fields (status defaults to {@code PLANNING}
     * when not specified).
     * <p>
     * When the resulting status is {@code PLANNING}, {@code datePlanned} is set to the
     * current timestamp automatically.
     *
     * @param userId the ID of the authenticated user
     * @param input  the input data for the create-or-update operation
     * @return the persisted (created or updated) {@link UserMediaListModel} entry
     */
    UserMediaListModel upsertUserMedia(Integer userId, UpdateUserMediaInput input);

    /**
     * Removes a media item from the user's library.
     *
     * @param userId  the ID of the authenticated user
     * @param mediaId the ID of the media to remove
     * @return {@code true} if the entry was found and deleted, {@code false} otherwise
     */
    boolean removeMedia(Integer userId, Integer mediaId);
}
