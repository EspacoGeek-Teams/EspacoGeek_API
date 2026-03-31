package com.espacogeek.geek.controllers;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import com.espacogeek.geek.exception.GenericException;
import com.espacogeek.geek.models.CategoryType;
import com.espacogeek.geek.models.UserMediaListModel;
import com.espacogeek.geek.services.UserMediaListService;
import com.espacogeek.geek.services.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class UserMediaListController {

    private final UserMediaListService userMediaListService;
    private final UserService userService;

    /**
     * Returns the authenticated user's media library, optionally filtered by
     * status, statusId, category, categoryName, genre, genreId, mediaId, media name,
     * or alternative title.
     *
     * @param status       optional user tracking status string filter (e.g. "watching")
     * @param statusId     optional media production status ID filter (MediaStatusModel.id)
     * @param categoryId   optional media category ID filter
     * @param categoryName optional media category name filter (e.g. "ANIME")
     * @param genreId      optional genre ID filter
     * @param genreName    optional genre name filter
     * @param mediaId      optional media ID filter
     * @param mediaName    optional media name filter (partial match)
     * @param altTitle     optional alternative title filter (partial match)
     * @param authentication the currently authenticated user (required)
     * @return list of matching library entries belonging to the user
     */
    @QueryMapping(name = "findUserMediaLibrary")
    @PreAuthorize("hasRole('user')")
    public List<UserMediaListModel> findUserMediaLibrary(
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
        var user = userService.findUserByEmail(authentication.getName())
                .orElseThrow(() -> new GenericException("User not found"));
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
                user.getId(), status, statusId, categoryId, categoryType, genreId, genreName, mediaId, mediaName, altTitle);
    }
}
