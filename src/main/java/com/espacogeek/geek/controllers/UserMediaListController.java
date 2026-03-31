package com.espacogeek.geek.controllers;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import com.espacogeek.geek.exception.AccessDeniedException;
import com.espacogeek.geek.exception.GenericException;
import com.espacogeek.geek.exception.NotFoundException;
import com.espacogeek.geek.models.CategoryType;
import com.espacogeek.geek.models.UserMediaListModel;
import com.espacogeek.geek.models.UserModel;
import com.espacogeek.geek.services.UserMediaListService;
import com.espacogeek.geek.services.UserService;
import com.espacogeek.geek.types.UpdateUserMediaInput;
import com.espacogeek.geek.utils.UserUtils;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class UserMediaListController {

    private final UserMediaListService userMediaListService;
    private final UserService userService;

    /**
     * Returns a user's media library, optionally filtered by status, statusId,
     * category, categoryName, genre, genreId, mediaId, media name, or alternative title.
     *
     * <p>If {@code userId} is omitted, returns the authenticated user's own library.
     * If {@code userId} is provided and different from the authenticated user, only
     * returns results if the target user's library is not marked as private.
     *
     * @param userId       optional target user ID; defaults to the authenticated user
     * @param status       optional user tracking status string filter (e.g. "IN_PROGRESS")
     * @param statusId     optional media production status ID filter (MediaStatusModel.id)
     * @param categoryId   optional media category ID filter
     * @param categoryName optional media category name filter (e.g. "ANIME")
     * @param genreId      optional genre ID filter
     * @param genreName    optional genre name filter
     * @param mediaId      optional media ID filter
     * @param mediaName    optional media name filter (partial match)
     * @param altTitle     optional alternative title filter (partial match)
     * @param authentication the currently authenticated user (required)
     * @return list of matching library entries belonging to the target user
     */
    @QueryMapping(name = "findUserMediaLibrary")
    @PreAuthorize("hasRole('user')")
    public List<UserMediaListModel> findUserMediaLibrary(
            @Argument(name = "userId") Integer userId,
            @Argument(name = "status") String status,
            @Argument(name = "statusId") Integer statusId,
            @Argument(name = "categoryId") Integer categoryId,
            @Argument(name = "categoryName") String categoryName,
            @Argument(name = "genreId") Integer genreId,
            @Argument(name = "genreName") String genreName,
            @Argument(name = "mediaId") Integer mediaId,
            @Argument(name = "mediaName") String mediaName,
            @Argument(name = "altTitle") String altTitle,
            Authentication authentication) {
        Integer authenticatedUserId = UserUtils.getUserID(authentication);

        Integer targetUserId;
        if (userId == null || userId.equals(authenticatedUserId)) {
            targetUserId = authenticatedUserId;
        } else {
            // Viewing another user's library: check privacy setting
            UserModel targetUser = userService.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User not found"));
            if (Boolean.TRUE.equals(targetUser.getPrivateList())) {
                throw new AccessDeniedException("This user's library is private");
            }
            targetUserId = userId;
        }

        status = status == null ? null : status.trim();
        genreName = genreName == null ? null : genreName.trim();
        mediaName = mediaName == null ? null : mediaName.trim();
        altTitle = altTitle == null ? null : altTitle.trim();
        String categoryNameTrimmed = categoryName == null ? null : categoryName.trim();
        CategoryType categoryType = null;
        if (categoryNameTrimmed != null) {
            try {
                categoryType = CategoryType.valueOf(categoryNameTrimmed.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new GenericException("Invalid categoryName: " + categoryNameTrimmed);
            }
        }
        return userMediaListService.findByUserIdWithFilters(
                targetUserId, status, statusId, categoryId, categoryType, genreId, genreName, mediaId, mediaName, altTitle);
    }

    /**
     * Adds the specified media to the authenticated user's library with default status
     * ({@code PLANNING}) and progress 0. Throws an error if the media is already
     * in the user's library.
     *
     * @param mediaId        the ID of the media to add
     * @param authentication the currently authenticated user
     * @return the newly created library entry
     */
    @MutationMapping(name = "addMediaToUserLibrary")
    @PreAuthorize("hasRole('user')")
    public UserMediaListModel addMediaToUserLibrary(
            @Argument(name = "mediaId") Integer mediaId,
            Authentication authentication) {
        Integer userId = UserUtils.getUserID(authentication);
        return userMediaListService.addMedia(userId, mediaId);
    }

    /**
     * Creates or updates the authenticated user's library entry for the media specified
     * in {@code input.mediaId} (upsert). Non-null fields in the input are applied to the
     * entry; absent fields are left unchanged on update. When the resulting status is
     * {@code PLANNING}, {@code datePlanned} is automatically set to the current timestamp.
     *
     * @param input          the create-or-update payload
     * @param authentication the currently authenticated user
     * @return the persisted (created or updated) library entry
     */
    @MutationMapping(name = "userMediaProgress")
    @PreAuthorize("hasRole('user')")
    public UserMediaListModel userMediaProgress(
            @Argument(name = "input") UpdateUserMediaInput input,
            Authentication authentication) {
        Integer userId = UserUtils.getUserID(authentication);
        return userMediaListService.userMediaProgress(userId, input);
    }

    /**
     * Removes the specified media from the authenticated user's library.
     *
     * @param mediaId        the ID of the media to remove
     * @param authentication the currently authenticated user
     * @return {@code true} if the entry was removed, {@code false} if not found
     */
    @MutationMapping(name = "removeMediaFromUserLibrary")
    @PreAuthorize("hasRole('user')")
    public Boolean removeMediaFromUserLibrary(
            @Argument(name = "mediaId") Integer mediaId,
            Authentication authentication) {
        Integer userId = UserUtils.getUserID(authentication);
        return userMediaListService.removeMedia(userId, mediaId);
    }
}
