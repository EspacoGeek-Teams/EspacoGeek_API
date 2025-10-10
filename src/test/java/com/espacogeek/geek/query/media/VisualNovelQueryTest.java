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
class VisualNovelQueryTest {

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
    void vn_ByName_ShouldReturnMediaPage() {
        // Given
        MediaModel vn1 = new MediaModel();
        vn1.setId(1);
        vn1.setName("Steins;Gate");

        MediaModel vn2 = new MediaModel();
        vn2.setId(2);
        vn2.setName("Steins;Gate 0");

        TypeReferenceModel typeRef = new TypeReferenceModel();
        MediaCategoryModel category = new MediaCategoryModel();

        when(typeReferenceService.findById(MediaDataController.IGDB_ID)).thenReturn(Optional.of(typeRef));
        when(mediaCategoryService.findById(MediaDataController.VN_ID)).thenReturn(Optional.of(category));
        when(genericMediaDataController.searchMedia(anyString(), any(), any(), any()))
                .thenReturn(Arrays.asList(vn1, vn2));

        // When & Then
        graphQlTester.document("""
                query {
                    vn(name: "Steins") {
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
                .path("vn")
                .entity(MediaPage.class)
                .satisfies(result -> {
                    assertThat(result.getContent()).hasSize(2);
                    assertThat(result.getTotalElements()).isEqualTo(2);
                });
    }

    @Test
    void vn_NoParameters_ShouldReturnEmptyPage() {
        // When & Then
        graphQlTester.document("""
                query {
                    vn {
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
                .path("vn")
                .entity(MediaPage.class)
                .satisfies(result -> {
                    assertThat(result.getContent()).isNull();
                });
    }
}
