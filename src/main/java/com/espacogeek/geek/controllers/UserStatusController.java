package com.espacogeek.geek.controllers;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import com.espacogeek.geek.models.UserCustomStatusModel;
import com.espacogeek.geek.services.UserCustomStatusService;
import com.espacogeek.geek.utils.UserUtils;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class UserStatusController {

    private final UserCustomStatusService userCustomStatusService;

    /**
     * Returns all custom tracking statuses created by the authenticated user.
     *
     * @param authentication the currently authenticated user
     * @return list of the user's custom status labels
     */
    @QueryMapping(name = "getUserCustomStatuses")
    @PreAuthorize("hasRole('user')")
    public List<UserCustomStatusModel> getUserCustomStatuses(Authentication authentication) {
        Integer userId = UserUtils.getUserID(authentication);
        return userCustomStatusService.findByUserId(userId);
    }

    /**
     * Creates a new custom tracking status label for the authenticated user.
     *
     * @param name           the label for the custom status (e.g. "Re-watching")
     * @param authentication the currently authenticated user
     * @return the newly created custom status
     */
    @MutationMapping(name = "createUserCustomStatus")
    @PreAuthorize("hasRole('user')")
    public UserCustomStatusModel createUserCustomStatus(
            @Argument(name = "name") String name,
            Authentication authentication) {
        Integer userId = UserUtils.getUserID(authentication);
        return userCustomStatusService.create(userId, name);
    }

    /**
     * Updates the name of an existing custom tracking status owned by the authenticated user.
     *
     * @param id             the ID of the custom status to update
     * @param name           the new label
     * @param authentication the currently authenticated user
     * @return the updated custom status
     */
    @MutationMapping(name = "updateUserCustomStatus")
    @PreAuthorize("hasRole('user')")
    public UserCustomStatusModel updateUserCustomStatus(
            @Argument(name = "id") Integer id,
            @Argument(name = "name") String name,
            Authentication authentication) {
        Integer userId = UserUtils.getUserID(authentication);
        return userCustomStatusService.update(userId, id, name);
    }

    /**
     * Deletes a custom tracking status owned by the authenticated user.
     *
     * @param id             the ID of the custom status to delete
     * @param authentication the currently authenticated user
     * @return {@code true} if deleted, {@code false} if not found
     */
    @MutationMapping(name = "deleteUserCustomStatus")
    @PreAuthorize("hasRole('user')")
    public Boolean deleteUserCustomStatus(
            @Argument(name = "id") Integer id,
            Authentication authentication) {
        Integer userId = UserUtils.getUserID(authentication);
        return userCustomStatusService.delete(userId, id);
    }
}
