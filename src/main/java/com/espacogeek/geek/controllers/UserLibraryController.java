package com.espacogeek.geek.controllers;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import com.espacogeek.geek.models.UserLibraryModel;
import com.espacogeek.geek.services.UserLibraryService;
import com.espacogeek.geek.utils.UserUtils;

import lombok.RequiredArgsConstructor;

/**
 * GraphQL controller for user media library operations.
 * All endpoints require the user to be authenticated ({@code hasRole('user')}).
 */
@Controller
@RequiredArgsConstructor
public class UserLibraryController {

    private final UserLibraryService userLibraryService;

    /**
     * Returns all media entries in the authenticated user's library.
     */
    @QueryMapping(name = "findUserMediaLibrary")
    @PreAuthorize("hasRole('user')")
    public List<UserLibraryModel> findUserMediaLibrary(Authentication authentication) {
        Integer userId = UserUtils.getUserID(authentication);
        return userLibraryService.findByUserId(userId);
    }

    /**
     * Adds the specified media to the authenticated user's library.
     *
     * @param mediaId ID of the media to add
     * @return the newly created library entry
     */
    @MutationMapping(name = "addMediaToUserLibrary")
    @PreAuthorize("hasRole('user')")
    public UserLibraryModel addMediaToUserLibrary(Authentication authentication, @Argument(name = "mediaId") Integer mediaId) {
        Integer userId = UserUtils.getUserID(authentication);
        return userLibraryService.addMedia(userId, mediaId);
    }

    /**
     * Removes the specified media from the authenticated user's library.
     *
     * @param mediaId ID of the media to remove
     * @return {@code true} if the entry was removed, {@code false} if it was not found
     */
    @MutationMapping(name = "removeMediaFromUserLibrary")
    @PreAuthorize("hasRole('user')")
    public Boolean removeMediaFromUserLibrary(Authentication authentication, @Argument(name = "mediaId") Integer mediaId) {
        Integer userId = UserUtils.getUserID(authentication);
        return userLibraryService.removeMedia(userId, mediaId);
    }
}
