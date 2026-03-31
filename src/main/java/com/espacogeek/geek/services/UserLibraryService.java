package com.espacogeek.geek.services;

import java.util.List;

import com.espacogeek.geek.models.UserLibraryModel;

/**
 * Service interface for managing a user's personal media library.
 */
public interface UserLibraryService {

    /**
     * Retrieves all media entries in the given user's library.
     *
     * @param userId the ID of the user
     * @return list of {@link UserLibraryModel} entries belonging to the user
     */
    List<UserLibraryModel> findByUserId(Integer userId);

    /**
     * Adds a media item to the user's library with default values
     * (status = "Planning", progress = 0).
     *
     * @param userId  the ID of the authenticated user
     * @param mediaId the ID of the media to add
     * @return the newly created {@link UserLibraryModel} entry
     */
    UserLibraryModel addMedia(Integer userId, Integer mediaId);

    /**
     * Removes a media item from the user's library.
     *
     * @param userId  the ID of the authenticated user
     * @param mediaId the ID of the media to remove
     * @return {@code true} if the entry existed and was deleted,
     *         {@code false} if no matching entry was found
     */
    boolean removeMedia(Integer userId, Integer mediaId);
}
