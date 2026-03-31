package com.espacogeek.geek.mutation.userlibrary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.espacogeek.geek.controllers.UserLibraryController;
import com.espacogeek.geek.exception.GenericException;
import com.espacogeek.geek.models.MediaModel;
import com.espacogeek.geek.models.TypeStatusModel;
import com.espacogeek.geek.models.UserLibraryModel;
import com.espacogeek.geek.models.UserModel;
import com.espacogeek.geek.services.UserLibraryService;

@GraphQlTest(UserLibraryController.class)
@ActiveProfiles("test")
class UserMediaLibraryMutationTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private UserLibraryService userLibraryService;

    // -------------------------------------------------------------------------
    // addMediaToUserLibrary
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = {"ROLE_user", "ID_1"})
    void addMediaToUserLibrary_ValidMediaId_ShouldReturnEntry() {
        // Given
        TypeStatusModel status = new TypeStatusModel();
        status.setId(1);
        status.setName("Planning");

        MediaModel media = new MediaModel();
        media.setId(10);
        media.setName("Test Media");

        UserModel user = new UserModel();
        user.setId(1);
        user.setUsername("testuser");

        UserLibraryModel entry = new UserLibraryModel();
        entry.setId(100);
        entry.setProgress(0);
        entry.setAddedAt(new Date());
        entry.setTypeStatus(status);
        entry.setMedia(media);
        entry.setUser(user);

        when(userLibraryService.addMedia(anyInt(), anyInt())).thenReturn(entry);

        // When & Then
        graphQlTester.document("""
                mutation {
                    addMediaToUserLibrary(mediaId: 10) {
                        id
                        progress
                        typeStatus {
                            id
                            name
                        }
                        media {
                            id
                            name
                        }
                    }
                }
                """)
                .execute()
                .path("addMediaToUserLibrary")
                .hasValue()
                .path("addMediaToUserLibrary.progress")
                .entity(Integer.class)
                .satisfies(progress -> assertThat(progress).isEqualTo(0))
                .path("addMediaToUserLibrary.typeStatus.name")
                .entity(String.class)
                .satisfies(name -> assertThat(name).isEqualTo("Planning"));

        verify(userLibraryService).addMedia(1, 10);
    }

    @Test
    @WithMockUser(authorities = {"ROLE_user", "ID_1"})
    void addMediaToUserLibrary_NonExistentMedia_ShouldReturnError() {
        // Given
        when(userLibraryService.addMedia(anyInt(), anyInt()))
                .thenThrow(new GenericException("404 NOT_FOUND"));

        // When & Then
        graphQlTester.document("""
                mutation {
                    addMediaToUserLibrary(mediaId: 999) {
                        id
                        progress
                    }
                }
                """)
                .execute()
                .errors()
                .satisfy(errors -> assertThat(errors).isNotEmpty());
    }

    @Test
    void addMediaToUserLibrary_Unauthenticated_ShouldReturnError() {
        // When & Then — no @WithMockUser, so the request is anonymous
        graphQlTester.document("""
                mutation {
                    addMediaToUserLibrary(mediaId: 10) {
                        id
                        progress
                    }
                }
                """)
                .execute()
                .errors()
                .satisfy(errors -> assertThat(errors).isNotEmpty());
    }

    // -------------------------------------------------------------------------
    // removeMediaFromUserLibrary
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = {"ROLE_user", "ID_1"})
    void removeMediaFromUserLibrary_ExistingEntry_ShouldReturnTrue() {
        // Given
        when(userLibraryService.removeMedia(anyInt(), anyInt())).thenReturn(true);

        // When & Then
        graphQlTester.document("""
                mutation {
                    removeMediaFromUserLibrary(mediaId: 10)
                }
                """)
                .execute()
                .path("removeMediaFromUserLibrary")
                .entity(Boolean.class)
                .satisfies(result -> assertThat(result).isTrue());

        verify(userLibraryService).removeMedia(1, 10);
    }

    @Test
    @WithMockUser(authorities = {"ROLE_user", "ID_1"})
    void removeMediaFromUserLibrary_NotInUserLibrary_ShouldReturnFalse() {
        // Given
        when(userLibraryService.removeMedia(anyInt(), anyInt())).thenReturn(false);

        // When & Then
        graphQlTester.document("""
                mutation {
                    removeMediaFromUserLibrary(mediaId: 999)
                }
                """)
                .execute()
                .path("removeMediaFromUserLibrary")
                .entity(Boolean.class)
                .satisfies(result -> assertThat(result).isFalse());
    }

    @Test
    void removeMediaFromUserLibrary_Unauthenticated_ShouldReturnError() {
        // When & Then — no @WithMockUser, so the request is anonymous
        graphQlTester.document("""
                mutation {
                    removeMediaFromUserLibrary(mediaId: 10)
                }
                """)
                .execute()
                .errors()
                .satisfy(errors -> assertThat(errors).isNotEmpty());
    }
}
