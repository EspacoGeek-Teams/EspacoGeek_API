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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.espacogeek.geek.controllers.MediaController;
import com.espacogeek.geek.data.MediaDataController;
import com.espacogeek.geek.models.MediaModel;
import com.espacogeek.geek.services.MediaCategoryService;
import com.espacogeek.geek.services.MediaService;
import com.espacogeek.geek.services.TypeReferenceService;
import com.espacogeek.geek.types.MediaPage;

@GraphQlTest(MediaController.class)
@ActiveProfiles("test")
public class AnimeQueryTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private MediaService mediaService;

    @MockitoBean(name = "animeController")
    private MediaDataController animeController;

    @MockitoBean(name = "genericMediaDataController")
    private MediaDataController genericMediaDataController;

    @MockitoBean
    private TypeReferenceService typeReferenceService;

    @MockitoBean
    private MediaCategoryService mediaCategoryService;

    @Test
    void anime_ByName_ShouldReturnMediaPage() {
        // Given
        MediaModel media1 = new MediaModel();
        media1.setId(1);
        media1.setName("Your Lie in April");

        MediaModel media2 = new MediaModel();
        media2.setId(2);
        media2.setName("Naruto");

        Page<MediaModel> page = new PageImpl<>(Arrays.asList(media1, media2));

        when(mediaService.findAnimeByIdOrName(any(), anyString(), any())).thenReturn(page);

        // When & Then
        graphQlTester.document("""
                query {
                    anime(name: "Your Lie in April") {
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
                .path("anime")
                .entity(MediaPage.class)
                .satisfies(result -> {
                    assertThat(result.getContent()).hasSize(2);
                    assertThat(result.getTotalElements()).isEqualTo(2);
                });
    }

    @Test
    void anime_NoParameters_ShouldReturnEmptyPage() {
        // When & Then
        graphQlTester.document("""
                query {
                    anime {
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
                .path("anime")
                .entity(MediaPage.class)
                .satisfies(result -> {
                    assertThat(result.getContent()).isNull();
                });
    }
}
