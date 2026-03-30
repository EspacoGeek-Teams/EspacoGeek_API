package com.espacogeek.geek.services;

import java.util.List;

import com.espacogeek.geek.models.UserMediaListModel;

/**
 * Interface for the UserMediaListService, which provides methods for managing
 * the user's personal media library.
 */
public interface UserMediaListService {

    /**
     * Retrieves all library entries for the given user, with optional filters.
     *
     * @param userId     the ID of the authenticated user
     * @param status     optional status filter (e.g. "watching", "completed")
     * @param categoryId optional media category ID filter
     * @param genreName  optional genre name filter
     * @param mediaName  optional media name filter (partial match)
     * @param altTitle   optional alternative title filter (partial match)
     * @return a list of matching {@link UserMediaListModel} entries
     */
    List<UserMediaListModel> findByUserIdWithFilters(
            Integer userId,
            String status,
            Integer categoryId,
            String genreName,
            String mediaName,
            String altTitle);
}
