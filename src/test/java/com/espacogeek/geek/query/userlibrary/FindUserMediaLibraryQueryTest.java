package com.espacogeek.geek.query.userlibrary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.espacogeek.geek.controllers.UserLibraryController;
import com.espacogeek.geek.models.MediaModel;
import com.espacogeek.geek.models.TypeStatusModel;
import com.espacogeek.geek.models.UserLibraryModel;
import com.espacogeek.geek.models.UserModel;
import com.espacogeek.geek.services.UserLibraryService;

@GraphQlTest(UserLibraryController.class)
@ActiveProfiles("test")
class FindUserMediaLibraryQueryTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private UserLibraryService userLibraryService;

    @Test
    @WithMockUser(authorities = {"ROLE_user", "ID_1"})
    void findUserMediaLibrary_WithEntries_ShouldReturnList() {
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

        when(userLibraryService.findByUserId(anyInt())).thenReturn(List.of(entry));

        // When & Then
        graphQlTester.document("""
                query {
                    findUserMediaLibrary {
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
                .path("findUserMediaLibrary")
                .entityList(UserLibraryModel.class)
                .satisfies(entries -> {
                    assertThat(entries).hasSize(1);
                });
    }

    @Test
    @WithMockUser(authorities = {"ROLE_user", "ID_1"})
    void findUserMediaLibrary_EmptyLibrary_ShouldReturnEmptyList() {
        // Given
        when(userLibraryService.findByUserId(anyInt())).thenReturn(Collections.emptyList());

        // When & Then
        graphQlTester.document("""
                query {
                    findUserMediaLibrary {
                        id
                        progress
                    }
                }
                """)
                .execute()
                .path("findUserMediaLibrary")
                .entityList(UserLibraryModel.class)
                .hasSize(0);
    }

    @Test
    void findUserMediaLibrary_Unauthenticated_ShouldReturnError() {
        // When & Then — no @WithMockUser, so the request is anonymous
        graphQlTester.document("""
                query {
                    findUserMediaLibrary {
                        id
                        progress
                    }
                }
                """)
                .execute()
                .errors()
                .satisfy(errors -> assertThat(errors).isNotEmpty());
    }
}
