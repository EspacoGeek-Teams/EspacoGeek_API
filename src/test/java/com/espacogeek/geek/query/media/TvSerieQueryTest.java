package com.espacogeek.geek.query.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ActiveProfiles;

import com.espacogeek.geek.controllers.MediaController;
import com.espacogeek.geek.data.MediaDataController;
import com.espacogeek.geek.data.api.MediaApi;
import com.espacogeek.geek.models.MediaModel;
import com.espacogeek.geek.services.MediaCategoryService;
import com.espacogeek.geek.services.MediaService;
import com.espacogeek.geek.services.TypeReferenceService;
import com.espacogeek.geek.types.MediaPage;

@GraphQlTest(MediaController.class)
@ActiveProfiles("test")
class TvSerieQueryTest {

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
    void tvserie_ByName_ShouldReturnMediaPage() {
        // Given
        MediaModel media1 = new MediaModel();
        media1.setId(1);
        media1.setName("Breaking Bad");

        MediaModel media2 = new MediaModel();
        media2.setId(2);
        media2.setName("Better Call Saul");

        Page<MediaModel> page = new PageImpl<>(Arrays.asList(media1, media2));

        when(mediaService.findSerieByIdOrName(any(), anyString(), any())).thenReturn(page);

        // When & Then
        graphQlTester.document("""
                query {
                    tvserie(name: "Breaking") {
                        totalPages
                        totalElements
                        number
                        size
                        content {
                            id
                            name
                        }
                    }
                }
                """)
                .execute()
                .path("tvserie")
                .entity(MediaPage.class)
                .satisfies(result -> {
                    assertThat(result.getContent()).hasSize(2);
                    assertThat(result.getTotalElements()).isEqualTo(2);
                });
    }

    @Test
    void tvserie_ById_ShouldReturnMediaPage() {
        // Given
        MediaModel media = new MediaModel();
        media.setId(1);
        media.setName("The Sopranos");

        Page<MediaModel> page = new PageImpl<>(Arrays.asList(media));

        when(mediaService.findSerieByIdOrName(anyInt(), any(), any())).thenReturn(page);

        // When & Then
        graphQlTester.document("""
                query {
                    tvserie(id: 1) {
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
                .path("tvserie")
                .entity(MediaPage.class)
                .satisfies(result -> {
                    assertThat(result.getContent()).hasSize(1);
                    assertThat(result.getContent().get(0).getName()).isEqualTo("The Sopranos");
                });
    }

    @Test
    void tvserie_NoParameters_ShouldReturnEmptyPage() {
        // When & Then
        graphQlTester.document("""
                query {
                    tvserie {
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
                .path("tvserie")
                .entity(MediaPage.class)
                .satisfies(result -> {
                    assertThat(result.getContent()).isNull();
                });
    }
}
