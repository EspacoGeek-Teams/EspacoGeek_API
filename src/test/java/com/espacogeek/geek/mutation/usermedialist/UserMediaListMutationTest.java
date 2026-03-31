package com.espacogeek.geek.mutation.usermedialist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.espacogeek.geek.controllers.UserMediaListController;
import com.espacogeek.geek.exception.GenericException;
import com.espacogeek.geek.exception.MediaAlreadyInLibraryException;
import com.espacogeek.geek.models.MediaCategoryModel;
import com.espacogeek.geek.models.CategoryType;
import com.espacogeek.geek.models.MediaModel;
import com.espacogeek.geek.models.StatusType;
import com.espacogeek.geek.models.UserMediaListModel;
import com.espacogeek.geek.models.UserModel;
import com.espacogeek.geek.services.UserMediaListService;
import com.espacogeek.geek.services.UserService;

@GraphQlTest(UserMediaListController.class)
@ActiveProfiles("test")
class UserMediaListMutationTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private UserMediaListService userMediaListService;

    @MockitoBean
    private UserService userService;

    // ---- helpers ----

    private UserMediaListModel stubEntry(Integer userId, Integer mediaId) {
        UserModel user = new UserModel();
        user.setId(userId);
        user.setUsername("testuser");

        MediaModel media = new MediaModel();
        media.setId(mediaId);
        media.setName("Test Media");
        media.setMediaCategory(new MediaCategoryModel(1, CategoryType.ANIME, null));

        UserMediaListModel entry = new UserMediaListModel();
        entry.setUser(user);
        entry.setMedia(media);
        entry.setStatus(StatusType.PLAN_TO_WATCH.name());
        entry.setProgress(0);
        return entry;
    }

    // ---- addMediaToUserLibrary tests ----

    @Test
    @WithMockUser(authorities = {"ROLE_user", "ID_1"})
    void addMediaToUserLibrary_Success_ShouldReturnEntry() {
        UserMediaListModel entry = stubEntry(1, 10);
        when(userMediaListService.addMedia(eq(1), eq(10))).thenReturn(entry);

        graphQlTester.document("""
                mutation {
                    addMediaToUserLibrary(mediaId: 10) {
                        status
                        progress
                        media { id name }
                    }
                }
                """)
                .execute()
                .errors().verify()
                .path("addMediaToUserLibrary.status").entity(String.class).isEqualTo("PLAN_TO_WATCH")
                .path("addMediaToUserLibrary.progress").entity(Integer.class).isEqualTo(0);

        verify(userMediaListService).addMedia(1, 10);
    }

    @Test
    @WithMockUser(authorities = {"ROLE_user", "ID_1"})
    void addMediaToUserLibrary_AlreadyInLibrary_ShouldReturnError() {
        when(userMediaListService.addMedia(anyInt(), anyInt()))
                .thenThrow(new MediaAlreadyInLibraryException());

        graphQlTester.document("""
                mutation {
                    addMediaToUserLibrary(mediaId: 10) {
                        status
                    }
                }
                """)
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors).isNotEmpty();
                    assertThat(errors.get(0).getExtensions().get("errorCode")).isEqualTo(2005);
                });
    }

    @Test
    @WithMockUser(authorities = {"ROLE_user", "ID_1"})
    void addMediaToUserLibrary_NonExistentMedia_ShouldReturnError() {
        when(userMediaListService.addMedia(anyInt(), anyInt()))
                .thenThrow(new GenericException("404 NOT_FOUND"));

        graphQlTester.document("""
                mutation {
                    addMediaToUserLibrary(mediaId: 999) {
                        status
                    }
                }
                """)
                .execute()
                .errors()
                .satisfy(errors -> assertThat(errors).isNotEmpty());
    }

    @Test
    void addMediaToUserLibrary_Unauthenticated_ShouldReturnError() {
        graphQlTester.document("""
                mutation {
                    addMediaToUserLibrary(mediaId: 10) {
                        status
                    }
                }
                """)
                .execute()
                .errors()
                .satisfy(errors -> assertThat(errors).isNotEmpty());
    }

    // ---- removeMediaFromUserLibrary tests ----

    @Test
    @WithMockUser(authorities = {"ROLE_user", "ID_1"})
    void removeMediaFromUserLibrary_ExistingEntry_ShouldReturnTrue() {
        when(userMediaListService.removeMedia(eq(1), eq(10))).thenReturn(true);

        graphQlTester.document("""
                mutation {
                    removeMediaFromUserLibrary(mediaId: 10)
                }
                """)
                .execute()
                .errors().verify()
                .path("removeMediaFromUserLibrary").entity(Boolean.class).isEqualTo(true);

        verify(userMediaListService).removeMedia(1, 10);
    }

    @Test
    @WithMockUser(authorities = {"ROLE_user", "ID_1"})
    void removeMediaFromUserLibrary_NotInLibrary_ShouldReturnFalse() {
        when(userMediaListService.removeMedia(anyInt(), anyInt())).thenReturn(false);

        graphQlTester.document("""
                mutation {
                    removeMediaFromUserLibrary(mediaId: 999)
                }
                """)
                .execute()
                .errors().verify()
                .path("removeMediaFromUserLibrary").entity(Boolean.class).isEqualTo(false);

        verify(userMediaListService).removeMedia(1, 999);
    }

    @Test
    void removeMediaFromUserLibrary_Unauthenticated_ShouldReturnError() {
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
