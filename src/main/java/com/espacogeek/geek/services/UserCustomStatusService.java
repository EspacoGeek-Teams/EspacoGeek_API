package com.espacogeek.geek.services;

import java.util.List;

import com.espacogeek.geek.models.UserCustomStatusModel;

/**
 * Service interface for managing user-defined custom tracking status labels.
 * Users can create custom statuses (e.g. "Re-watching", "Collecting") in addition
 * to the default system statuses (PLANNING, IN_PROGRESS, COMPLETED, DROPPED, PAUSED).
 */
public interface UserCustomStatusService {

    /**
     * Returns all custom statuses belonging to the given user.
     *
     * @param userId the authenticated user's ID
     * @return list of the user's custom status entries
     */
    List<UserCustomStatusModel> findByUserId(Integer userId);

    /**
     * Creates a new custom status for the user.
     *
     * @param userId the authenticated user's ID
     * @param name   the label for the custom status
     * @return the persisted {@link UserCustomStatusModel}
     */
    UserCustomStatusModel create(Integer userId, String name);

    /**
     * Updates the name of an existing custom status owned by the user.
     *
     * @param userId   the authenticated user's ID
     * @param statusId the ID of the status to update
     * @param name     the new label
     * @return the updated {@link UserCustomStatusModel}
     */
    UserCustomStatusModel update(Integer userId, Integer statusId, String name);

    /**
     * Deletes a custom status owned by the user.
     *
     * @param userId   the authenticated user's ID
     * @param statusId the ID of the status to delete
     * @return {@code true} if deleted, {@code false} if not found
     */
    boolean delete(Integer userId, Integer statusId);
}
