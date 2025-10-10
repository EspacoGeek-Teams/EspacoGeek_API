package com.espacogeek.geek.query.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ActiveProfiles;

import com.espacogeek.geek.controllers.MediaController;
import com.espacogeek.geek.data.MediaDataController;
import com.espacogeek.geek.data.api.MediaApi;
import com.espacogeek.geek.models.MediaCategoryModel;
import com.espacogeek.geek.models.MediaModel;
import com.espacogeek.geek.models.TypeReferenceModel;
import com.espacogeek.geek.services.MediaCategoryService;
import com.espacogeek.geek.services.MediaService;
import com.espacogeek.geek.services.TypeReferenceService;
import com.espacogeek.geek.types.MediaPage;

@GraphQlTest(MediaController.class)
@ActiveProfiles("test")
class GameQueryTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockBean
    private MediaService mediaService;

    @MockBean(name = "serieController")
    private MediaDataController serieController;

    @MockBean(name = "genericMediaDataController")
    private MediaDataController genericMediaDataController;

    @MockBean
    private MediaApi gamesAndVNsAPI;

    @MockBean
    private TypeReferenceService typeReferenceService;

    @MockBean
    private MediaCategoryService mediaCategoryService;

    @Test
    void game_ByName_ShouldReturnMediaPage() {
        // Given
        MediaModel game1 = new MediaModel();
        game1.setId(1);
        game1.setName("The Witcher 3");

        MediaModel game2 = new MediaModel();
        game2.setId(2);
        game2.setName("Witcher 2");

        TypeReferenceModel typeRef = new TypeReferenceModel();
        MediaCategoryModel category = new MediaCategoryModel();

        when(typeReferenceService.findById(MediaDataController.IGDB_ID)).thenReturn(Optional.of(typeRef));
        when(mediaCategoryService.findById(MediaDataController.GAME_ID)).thenReturn(Optional.of(category));
        when(genericMediaDataController.searchMedia(anyString(), any(), any(), any()))
                .thenReturn(Arrays.asList(game1, game2));

        // When & Then
        graphQlTester.document("""
                query {
                    game(name: "Witcher") {
                        totalPages
                        totalElements
                        content {
                            id
                            name
                        }
                    }
                }
                """)
                .execute()
                .path("game")
                .entity(MediaPage.class)
                .satisfies(result -> {
                    assertThat(result.getContent()).hasSize(2);
                    assertThat(result.getTotalElements()).isEqualTo(2);
                });
    }

    @Test
    void game_NoParameters_ShouldReturnEmptyPage() {
        // When & Then
        graphQlTester.document("""
                query {
                    game {
                        totalPages
                        totalElements
                        content {
                            id
                            name
                        }
                    }
                }
                """)
                .execute()
                .path("game")
                .entity(MediaPage.class)
                .satisfies(result -> {
                    assertThat(result.getContent()).isNull();
                });
    }
}
