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
import com.espacogeek.geek.data.api.MediaApi;
import com.espacogeek.geek.models.MediaModel;
import com.espacogeek.geek.services.MediaCategoryService;
import com.espacogeek.geek.services.MediaService;
import com.espacogeek.geek.services.TypeReferenceService;
import com.espacogeek.geek.types.MediaPage;
import com.espacogeek.geek.types.MediaSimplefied;

@GraphQlTest(MediaController.class)
@ActiveProfiles("test")
public class MovieQueryTest {
    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private MediaService mediaService;

    @MockitoBean(name = "movieController")
    private MediaDataController movieController;

    @MockitoBean(name = "genericMediaDataController")
    private MediaDataController genericMediaDataController;

    @MockitoBean
    private TypeReferenceService typeReferenceService;

    @MockitoBean
    private MediaCategoryService mediaCategoryService;

    @Test
    void movie_NoParameters_ShouldReturnEmptyPage() {
        // When & Then
        graphQlTester.document("""
                query {
                    movie {
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
                .path("movie")
                .entity(MediaPage.class)
                .satisfies(result -> {
                    assertThat(result.getContent()).isNull();
                });
    }
}
