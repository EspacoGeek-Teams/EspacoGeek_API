package com.espacogeek.geek.query.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Optional;

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
import com.espacogeek.geek.models.MediaCategoryModel;
import com.espacogeek.geek.models.MediaModel;
import com.espacogeek.geek.models.TypeReferenceModel;
import com.espacogeek.geek.services.MediaCategoryService;
import com.espacogeek.geek.services.MediaService;
import com.espacogeek.geek.services.TypeReferenceService;

@GraphQlTest(MediaController.class)
@ActiveProfiles("test")
class MediaQueryTest {

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
    void media_ById_ShouldReturnMedia() {
        // Given
        MediaModel media = new MediaModel();
        media.setId(1);
        media.setName("Test Media");
        
        MediaCategoryModel category = new MediaCategoryModel();
        category.setId(1);
        media.setMediaCategory(category);

        when(mediaService.findByIdEager(anyInt())).thenReturn(Optional.of(media));

        // When & Then
        graphQlTester.document("""
                query {
                    media(id: 1) {
                        id
                        name
                    }
                }
                """)
                .execute()
                .path("media")
                .entity(MediaModel.class)
                .satisfies(result -> {
                    assertThat(result.getId()).isEqualTo(1);
                    assertThat(result.getName()).isEqualTo("Test Media");
                });
    }

    @Test
    void media_NotFound_ShouldReturnError() {
        // Given
        when(mediaService.findByIdEager(anyInt())).thenReturn(Optional.empty());

        // When & Then
        graphQlTester.document("""
                query {
                    media(id: 999) {
                        id
                        name
                    }
                }
                """)
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors).isNotEmpty();
                });
    }
}
