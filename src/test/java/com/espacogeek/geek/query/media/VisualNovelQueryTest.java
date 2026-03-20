package com.espacogeek.geek.query.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ActiveProfiles;

import com.espacogeek.geek.controllers.MediaController;
import com.espacogeek.geek.data.MediaDataController;
import com.espacogeek.geek.data.api.MediaApi;
import com.espacogeek.geek.repositories.AlternativeTitlesRepository;
import com.espacogeek.geek.repositories.ExternalReferenceRepository;
import com.espacogeek.geek.repositories.MediaRepository;
import com.espacogeek.geek.repositories.SeasonRepository;
import com.espacogeek.geek.services.MediaCategoryService;
import com.espacogeek.geek.services.MediaService;
import com.espacogeek.geek.services.TypeReferenceService;
import com.espacogeek.geek.types.MediaPage;
import com.espacogeek.geek.types.MediaSimplefied;

@GraphQlTest(MediaController.class)
@ActiveProfiles("test")
class VisualNovelQueryTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private MediaService mediaService;

    @MockitoBean(name = "serieController")
    private MediaDataController serieController;

    @MockitoBean(name = "genericMediaDataController")
    private MediaDataController genericMediaDataController;

    @MockitoBean
    private MediaApi gamesAndVNsAPI;

    @MockitoBean
    private TypeReferenceService typeReferenceService;

    @MockitoBean
    private MediaCategoryService mediaCategoryService;

    @MockitoBean
    private SeasonRepository seasonRepository;

    @MockitoBean
    private AlternativeTitlesRepository alternativeTitlesRepository;

    @MockitoBean
    private ExternalReferenceRepository externalReferenceRepository;

    @MockitoBean
    private MediaRepository mediaRepository;

    private MediaPage stubMediaPage() {
        MediaSimplefied item = new MediaSimplefied();
        item.setId(2);
        item.setName("Steins;Gate");
        item.setCover("https://example.com/steinsgate.jpg");

        MediaPage page = new MediaPage();
        page.setContent(List.of(item));
        page.setTotalElements(1);
        page.setTotalPages(1);
        return page;
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

    @Test
    void vn_WithName_ShouldReturnResults() {
        // Given
        when(mediaService.findVisualNovelByIdOrName(isNull(), eq("steins"), any()))
                .thenReturn(stubMediaPage());

        // When & Then
        graphQlTester.document("""
                query MediaPage($name: String) {
                    vn(name: $name) {
                        totalPages
                        totalElements
                        content {
                            id
                            name
                            cover
                        }
                    }
                }
                """)
                .variable("name", "steins")
                .execute()
                .path("vn")
                .entity(MediaPage.class)
                .satisfies(result -> {
                    assertThat(result.getContent()).isNotNull();
                    assertThat(result.getContent()).hasSize(1);
                    assertThat(result.getContent().get(0).getName()).isEqualTo("Steins;Gate");
                    assertThat(result.getTotalElements()).isEqualTo(1);
                });
    }

    @Test
    void vn_WithName_ShouldNotReturnErrors() {
        // Given
        when(mediaService.findVisualNovelByIdOrName(isNull(), eq("steins"), any()))
                .thenReturn(stubMediaPage());

        // When & Then
        graphQlTester.document("""
                query MediaPage($name: String) {
                    vn(name: $name) {
                        content {
                            id
                            name
                        }
                    }
                }
                """)
                .variable("name", "steins")
                .execute()
                .errors()
                .verify();
    }
}
