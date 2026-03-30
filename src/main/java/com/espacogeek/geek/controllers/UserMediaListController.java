package com.espacogeek.geek.controllers;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import com.espacogeek.geek.exception.GenericException;
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
     * status, category, genre, media name, or alternative title.
     *
     * @param status     optional status filter (e.g. "watching", "completed")
     * @param categoryId optional media category ID filter
     * @param genreName  optional genre name filter
     * @param mediaName  optional media name filter (partial match)
     * @param altTitle   optional alternative title filter (partial match)
     * @param authentication the currently authenticated user (required)
     * @return list of matching library entries belonging to the user
     */
    @QueryMapping(name = "findUserMediaLibrary")
    @PreAuthorize("hasRole('user')")
    public List<UserMediaListModel> findUserMediaLibrary(
            @Argument(name = "status") String status,
            @Argument(name = "categoryId") Integer categoryId,
            @Argument(name = "genreName") String genreName,
            @Argument(name = "mediaName") String mediaName,
            @Argument(name = "altTitle") String altTitle,
            Authentication authentication) {
        var user = userService.findUserByEmail(authentication.getName())
                .orElseThrow(() -> new GenericException("User not found"));
        return userMediaListService.findByUserIdWithFilters(user.getId(), status, categoryId, genreName, mediaName, altTitle);
    }
}
