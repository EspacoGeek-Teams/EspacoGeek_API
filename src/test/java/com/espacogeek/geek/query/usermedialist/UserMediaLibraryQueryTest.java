package com.espacogeek.geek.query.usermedialist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.espacogeek.geek.controllers.UserMediaListController;
import com.espacogeek.geek.models.CategoryType;
import com.espacogeek.geek.models.MediaCategoryModel;
import com.espacogeek.geek.models.MediaModel;
import com.espacogeek.geek.models.UserMediaListModel;
import com.espacogeek.geek.models.UserModel;
import com.espacogeek.geek.services.UserMediaListService;
import com.espacogeek.geek.services.UserService;

@GraphQlTest(UserMediaListController.class)
@ActiveProfiles("test")
class UserMediaLibraryQueryTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private UserMediaListService userMediaListService;

    @MockitoBean
    private UserService userService;

    // ---- helpers ----

    private UserModel stubUser(Integer id, String email) {
        UserModel user = new UserModel();
        user.setId(id);
        user.setUsername("testuser");
        user.setEmail(email);
        user.setPassword("password".getBytes());
        return user;
    }

    private MediaModel stubMedia(Integer id, String name) {
        MediaCategoryModel category = new MediaCategoryModel(1, CategoryType.ANIME, null);
        MediaModel media = new MediaModel();
        media.setId(id);
        media.setName(name);
        media.setMediaCategory(category);
        return media;
    }

    private UserMediaListModel stubEntry(UserModel user, MediaModel media, String status) {
        UserMediaListModel entry = new UserMediaListModel();
        entry.setUser(user);
        entry.setMedia(media);
        entry.setStatus(status);
        entry.setProgress(3);
        entry.setScore(8.5f);
        return entry;
    }

    // ---- tests ----

    @Test
    @WithMockUser(authorities = {"ROLE_user", "ID_1"})
    void findUserMediaLibrary_NoFilters_ShouldReturnAllUserEntries() {
        UserModel user = stubUser(1, "user@example.com");
        MediaModel media = stubMedia(10, "Naruto");
        UserMediaListModel entry = stubEntry(user, media, "watching");

        when(userMediaListService.findByUserIdWithFilters(
                eq(1), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(entry));

        graphQlTester.document("""
                query {
                    findUserMediaLibrary {
                        status
                        progress
                        score
                        media { id name }
                    }
                }
                """)
                .execute()
                .errors().verify()
                .path("findUserMediaLibrary").entityList(UserMediaListModel.class)
                .satisfies(results -> {
                    assertThat(results).hasSize(1);
                    assertThat(results.get(0).getStatus()).isEqualTo("watching");
                    assertThat(results.get(0).getProgress()).isEqualTo(3);
                });

        verify(userMediaListService).findByUserIdWithFilters(1, null, null, null, null, null, null, null, null, null);
    }

    @Test
    @WithMockUser(authorities = {"ROLE_user", "ID_1"})
    void findUserMediaLibrary_FilterByStatus_ShouldPassStatusToService() {
        UserModel user = stubUser(1, "user@example.com");
        MediaModel media = stubMedia(10, "Attack on Titan");
        UserMediaListModel entry = stubEntry(user, media, "completed");

        when(userMediaListService.findByUserIdWithFilters(
                eq(1), eq("completed"), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(entry));

        graphQlTester.document("""
                query {
                    findUserMediaLibrary(status: "completed") {
                        status
                        media { name }
                    }
                }
                """)
                .execute()
                .errors().verify()
                .path("findUserMediaLibrary[0].status").entity(String.class).isEqualTo("completed");

        verify(userMediaListService).findByUserIdWithFilters(1, "completed", null, null, null, null, null, null, null, null);
    }

    @Test
    @WithMockUser(authorities = {"ROLE_user", "ID_1"})
    void findUserMediaLibrary_FilterByCategoryName_ShouldPassCategoryNameToService() {
        UserModel user = stubUser(1, "user@example.com");
        MediaModel media = stubMedia(20, "Dragon Ball");
        UserMediaListModel entry = stubEntry(user, media, "watching");

        when(userMediaListService.findByUserIdWithFilters(
                eq(1), isNull(), isNull(), isNull(), eq(CategoryType.ANIME), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(entry));

        graphQlTester.document("""
                query {
                    findUserMediaLibrary(categoryName: "ANIME") {
                        status
                        media { name }
                    }
                }
                """)
                .execute()
                .errors().verify()
                .path("findUserMediaLibrary[0].media.name").entity(String.class).isEqualTo("Dragon Ball");

        verify(userMediaListService).findByUserIdWithFilters(1, null, null, null, CategoryType.ANIME, null, null, null, null, null);
    }

    @Test
    @WithMockUser(authorities = {"ROLE_user", "ID_1"})
    void findUserMediaLibrary_AllFilters_ShouldPassAllFiltersToService() {
        when(userMediaListService.findByUserIdWithFilters(
                eq(1), eq("watching"), eq(3), eq(1), eq(CategoryType.ANIME), eq(5), eq("Action"), eq(99), eq("Naruto"), eq("Shippuden")))
                .thenReturn(List.of());

        graphQlTester.document("""
                query {
                    findUserMediaLibrary(
                        status: "watching",
                        statusId: 3,
                        categoryId: 1,
                        categoryName: "ANIME",
                        genreId: 5,
                        genreName: "Action",
                        mediaId: 99,
                        mediaName: "Naruto",
                        altTitle: "Shippuden"
                    ) {
                        status
                        media { name }
                    }
                }
                """)
                .execute()
                .errors().verify()
                .path("findUserMediaLibrary").entityList(UserMediaListModel.class)
                .satisfies(results -> assertThat(results).isEmpty());

        verify(userMediaListService).findByUserIdWithFilters(1, "watching", 3, 1, CategoryType.ANIME, 5, "Action", 99, "Naruto", "Shippuden");
    }

    @Test
    @WithMockUser(authorities = {"ROLE_user", "ID_1"})
    void findUserMediaLibrary_WhenEmpty_ShouldReturnEmptyList() {
        when(userMediaListService.findByUserIdWithFilters(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        graphQlTester.document("""
                query {
                    findUserMediaLibrary {
                        status
                        media { name }
                    }
                }
                """)
                .execute()
                .errors().verify()
                .path("findUserMediaLibrary").entityList(UserMediaListModel.class)
                .satisfies(results -> assertThat(results).isEmpty());

        verify(userMediaListService).findByUserIdWithFilters(1, null, null, null, null, null, null, null, null, null);
    }

    @Test
    @WithMockUser(authorities = {"ROLE_user", "ID_1"})
    void findUserMediaLibrary_MultipleEntries_ShouldReturnAll() {
        UserModel user = stubUser(1, "user@example.com");
        MediaModel media1 = stubMedia(10, "Naruto");
        MediaModel media2 = stubMedia(20, "Bleach");
        UserMediaListModel entry1 = stubEntry(user, media1, "completed");
        UserMediaListModel entry2 = stubEntry(user, media2, "watching");

        when(userMediaListService.findByUserIdWithFilters(
                eq(1), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(entry1, entry2));

        graphQlTester.document("""
                query {
                    findUserMediaLibrary {
                        status
                        media { name }
                    }
                }
                """)
                .execute()
                .errors().verify()
                .path("findUserMediaLibrary").entityList(UserMediaListModel.class)
                .satisfies(results -> {
                    assertThat(results).hasSize(2);
                    assertThat(results).extracting("status").containsExactlyInAnyOrder("completed", "watching");
                });

        verify(userMediaListService).findByUserIdWithFilters(1, null, null, null, null, null, null, null, null, null);
    }

    @Test
    @WithMockUser(authorities = {"ROLE_user", "ID_1"})
    void findUserMediaLibrary_WithUserId_PublicList_ShouldReturnEntries() {
        // User 2 has a public library
        UserModel otherUser = stubUser(2, "other@example.com");
        otherUser.setPrivateList(false);
        MediaModel media = stubMedia(10, "Naruto");
        UserMediaListModel entry = stubEntry(otherUser, media, "watching");

        when(userService.findById(2)).thenReturn(Optional.of(otherUser));
        when(userMediaListService.findByUserIdWithFilters(
                eq(2), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(entry));

        graphQlTester.document("""
                query {
                    findUserMediaLibrary(userId: 2) {
                        status
                        media { name }
                    }
                }
                """)
                .execute()
                .errors().verify()
                .path("findUserMediaLibrary[0].status").entity(String.class).isEqualTo("watching");

        verify(userMediaListService).findByUserIdWithFilters(2, null, null, null, null, null, null, null, null, null);
    }

    @Test
    @WithMockUser(authorities = {"ROLE_user", "ID_1"})
    void findUserMediaLibrary_WithUserId_PrivateList_ShouldReturnAccessDeniedError() {
        // User 2 has a private library
        UserModel otherUser = stubUser(2, "other@example.com");
        otherUser.setPrivateList(true);

        when(userService.findById(2)).thenReturn(Optional.of(otherUser));

        graphQlTester.document("""
                query {
                    findUserMediaLibrary(userId: 2) {
                        status
                    }
                }
                """)
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors).isNotEmpty();
                    assertThat(errors.get(0).getExtensions().get("errorCode")).isEqualTo(3403);
                });
    }

    @Test
    void findUserMediaLibrary_WithoutAuth_ShouldReturnError() {
        // No @WithMockUser — unauthenticated request should be rejected
        graphQlTester.document("""
                query {
                    findUserMediaLibrary {
                        status
                        media { name }
                    }
                }
                """)
                .execute()
                .errors()
                .satisfy(errors -> assertThat(errors).isNotEmpty());
    }
}
