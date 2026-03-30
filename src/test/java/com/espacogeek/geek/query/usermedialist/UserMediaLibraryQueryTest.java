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
    @WithMockUser(username = "user@example.com", roles = {"user"})
    void findUserMediaLibrary_NoFilters_ShouldReturnAllUserEntries() {
        UserModel user = stubUser(1, "user@example.com");
        MediaModel media = stubMedia(10, "Naruto");
        UserMediaListModel entry = stubEntry(user, media, "watching");

        when(userService.findUserByEmail("user@example.com")).thenReturn(Optional.of(user));
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
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = {"user"})
    void findUserMediaLibrary_FilterByStatus_ShouldPassStatusToService() {
        UserModel user = stubUser(1, "user@example.com");
        MediaModel media = stubMedia(10, "Attack on Titan");
        UserMediaListModel entry = stubEntry(user, media, "completed");

        when(userService.findUserByEmail("user@example.com")).thenReturn(Optional.of(user));
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

        verify(userMediaListService).findByUserIdWithFilters(
                1, "completed", null, null, null, null, null, null, null, null);
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = {"user"})
    void findUserMediaLibrary_FilterByStatusId_ShouldPassStatusIdToService() {
        UserModel user = stubUser(1, "user@example.com");
        MediaModel media = stubMedia(10, "Steins;Gate");
        UserMediaListModel entry = stubEntry(user, media, "completed");

        when(userService.findUserByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(userMediaListService.findByUserIdWithFilters(
                eq(1), isNull(), eq(2), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(entry));

        graphQlTester.document("""
                query {
                    findUserMediaLibrary(statusId: 2) {
                        status
                        media { name }
                    }
                }
                """)
                .execute()
                .errors().verify()
                .path("findUserMediaLibrary[0].media.name").entity(String.class).isEqualTo("Steins;Gate");

        verify(userMediaListService).findByUserIdWithFilters(
                1, null, 2, null, null, null, null, null, null, null);
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = {"user"})
    void findUserMediaLibrary_FilterByCategoryId_ShouldPassCategoryIdToService() {
        UserModel user = stubUser(1, "user@example.com");
        MediaModel media = stubMedia(20, "One Piece");
        UserMediaListModel entry = stubEntry(user, media, "watching");

        when(userService.findUserByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(userMediaListService.findByUserIdWithFilters(
                eq(1), isNull(), isNull(), eq(2), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(entry));

        graphQlTester.document("""
                query {
                    findUserMediaLibrary(categoryId: 2) {
                        status
                        media { name }
                    }
                }
                """)
                .execute()
                .errors().verify()
                .path("findUserMediaLibrary[0].media.name").entity(String.class).isEqualTo("One Piece");

        verify(userMediaListService).findByUserIdWithFilters(
                1, null, null, 2, null, null, null, null, null, null);
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = {"user"})
    void findUserMediaLibrary_FilterByCategoryName_ShouldPassCategoryNameToService() {
        UserModel user = stubUser(1, "user@example.com");
        MediaModel media = stubMedia(20, "Dragon Ball");
        UserMediaListModel entry = stubEntry(user, media, "watching");

        when(userService.findUserByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(userMediaListService.findByUserIdWithFilters(
                eq(1), isNull(), isNull(), isNull(), eq("ANIME"), isNull(), isNull(), isNull(), isNull(), isNull()))
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

        verify(userMediaListService).findByUserIdWithFilters(
                1, null, null, null, "ANIME", null, null, null, null, null);
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = {"user"})
    void findUserMediaLibrary_FilterByGenreId_ShouldPassGenreIdToService() {
        UserModel user = stubUser(1, "user@example.com");
        MediaModel media = stubMedia(30, "Demon Slayer");
        UserMediaListModel entry = stubEntry(user, media, "watching");

        when(userService.findUserByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(userMediaListService.findByUserIdWithFilters(
                eq(1), isNull(), isNull(), isNull(), isNull(), eq(5), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(entry));

        graphQlTester.document("""
                query {
                    findUserMediaLibrary(genreId: 5) {
                        status
                        media { name }
                    }
                }
                """)
                .execute()
                .errors().verify()
                .path("findUserMediaLibrary[0].media.name").entity(String.class).isEqualTo("Demon Slayer");

        verify(userMediaListService).findByUserIdWithFilters(
                1, null, null, null, null, 5, null, null, null, null);
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = {"user"})
    void findUserMediaLibrary_FilterByGenreName_ShouldPassGenreNameToService() {
        UserModel user = stubUser(1, "user@example.com");
        MediaModel media = stubMedia(30, "Demon Slayer");
        UserMediaListModel entry = stubEntry(user, media, "watching");

        when(userService.findUserByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(userMediaListService.findByUserIdWithFilters(
                eq(1), isNull(), isNull(), isNull(), isNull(), isNull(), eq("Action"), isNull(), isNull(), isNull()))
                .thenReturn(List.of(entry));

        graphQlTester.document("""
                query {
                    findUserMediaLibrary(genreName: "Action") {
                        status
                        media { name }
                    }
                }
                """)
                .execute()
                .errors().verify()
                .path("findUserMediaLibrary[0].media.name").entity(String.class).isEqualTo("Demon Slayer");

        verify(userMediaListService).findByUserIdWithFilters(
                1, null, null, null, null, null, "Action", null, null, null);
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = {"user"})
    void findUserMediaLibrary_FilterByMediaId_ShouldPassMediaIdToService() {
        UserModel user = stubUser(1, "user@example.com");
        MediaModel media = stubMedia(42, "Fullmetal Alchemist");
        UserMediaListModel entry = stubEntry(user, media, "completed");

        when(userService.findUserByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(userMediaListService.findByUserIdWithFilters(
                eq(1), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(42), isNull(), isNull()))
                .thenReturn(List.of(entry));

        graphQlTester.document("""
                query {
                    findUserMediaLibrary(mediaId: 42) {
                        status
                        media { id name }
                    }
                }
                """)
                .execute()
                .errors().verify()
                .path("findUserMediaLibrary[0].media.name").entity(String.class).isEqualTo("Fullmetal Alchemist");

        verify(userMediaListService).findByUserIdWithFilters(
                1, null, null, null, null, null, null, 42, null, null);
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = {"user"})
    void findUserMediaLibrary_FilterByMediaName_ShouldPassMediaNameToService() {
        UserModel user = stubUser(1, "user@example.com");
        MediaModel media = stubMedia(40, "Fullmetal Alchemist");
        UserMediaListModel entry = stubEntry(user, media, "completed");

        when(userService.findUserByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(userMediaListService.findByUserIdWithFilters(
                eq(1), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq("Fullmetal"), isNull()))
                .thenReturn(List.of(entry));

        graphQlTester.document("""
                query {
                    findUserMediaLibrary(mediaName: "Fullmetal") {
                        status
                        media { name }
                    }
                }
                """)
                .execute()
                .errors().verify()
                .path("findUserMediaLibrary[0].media.name").entity(String.class).isEqualTo("Fullmetal Alchemist");

        verify(userMediaListService).findByUserIdWithFilters(
                1, null, null, null, null, null, null, null, "Fullmetal", null);
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = {"user"})
    void findUserMediaLibrary_FilterByAltTitle_ShouldPassAltTitleToService() {
        UserModel user = stubUser(1, "user@example.com");
        MediaModel media = stubMedia(50, "Shingeki no Kyojin");
        UserMediaListModel entry = stubEntry(user, media, "completed");

        when(userService.findUserByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(userMediaListService.findByUserIdWithFilters(
                eq(1), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq("Attack")))
                .thenReturn(List.of(entry));

        graphQlTester.document("""
                query {
                    findUserMediaLibrary(altTitle: "Attack") {
                        status
                        media { name }
                    }
                }
                """)
                .execute()
                .errors().verify()
                .path("findUserMediaLibrary[0].media.name").entity(String.class).isEqualTo("Shingeki no Kyojin");

        verify(userMediaListService).findByUserIdWithFilters(
                1, null, null, null, null, null, null, null, null, "Attack");
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = {"user"})
    void findUserMediaLibrary_AllFilters_ShouldPassAllFiltersToService() {
        UserModel user = stubUser(1, "user@example.com");

        when(userService.findUserByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(userMediaListService.findByUserIdWithFilters(
                eq(1), eq("watching"), eq(3), eq(1), eq("ANIME"), eq(5), eq("Action"), eq(99), eq("Naruto"), eq("Shippuden")))
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

        verify(userMediaListService).findByUserIdWithFilters(
                1, "watching", 3, 1, "ANIME", 5, "Action", 99, "Naruto", "Shippuden");
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = {"user"})
    void findUserMediaLibrary_WhenEmpty_ShouldReturnEmptyList() {
        UserModel user = stubUser(1, "user@example.com");

        when(userService.findUserByEmail("user@example.com")).thenReturn(Optional.of(user));
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
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = {"user"})
    void findUserMediaLibrary_MultipleEntries_ShouldReturnAll() {
        UserModel user = stubUser(1, "user@example.com");
        MediaModel media1 = stubMedia(10, "Naruto");
        MediaModel media2 = stubMedia(20, "Bleach");
        UserMediaListModel entry1 = stubEntry(user, media1, "completed");
        UserMediaListModel entry2 = stubEntry(user, media2, "watching");

        when(userService.findUserByEmail("user@example.com")).thenReturn(Optional.of(user));
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
