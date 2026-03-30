package com.espacogeek.geek.query.mediastatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.espacogeek.geek.controllers.MediaStatusController;
import com.espacogeek.geek.models.MediaStatusModel;
import com.espacogeek.geek.models.StatusType;
import com.espacogeek.geek.services.MediaStatusService;

@GraphQlTest(MediaStatusController.class)
@ActiveProfiles("test")
class MediaStatusQueryTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private MediaStatusService mediaStatusService;

    @Test
    void findAllMediaStatuses_ShouldReturnAllStatuses() {
        // Given
        MediaStatusModel watching = new MediaStatusModel(1L, StatusType.WATCHING, null);
        MediaStatusModel completed = new MediaStatusModel(2L, StatusType.COMPLETED, null);
        MediaStatusModel planToWatch = new MediaStatusModel(3L, StatusType.PLAN_TO_WATCH, null);
        MediaStatusModel dropped = new MediaStatusModel(4L, StatusType.DROPPED, null);

        when(mediaStatusService.findAll()).thenReturn(List.of(watching, completed, planToWatch, dropped));

        // When & Then
        graphQlTester.document("""
                query {
                    findAllMediaStatuses {
                        id
                        name
                    }
                }
                """)
                .execute()
                .errors().verify()
                .path("findAllMediaStatuses").entityList(MediaStatusModel.class)
                .satisfies(result -> {
                    assertThat(result).hasSize(4);
                    assertThat(result.get(0).getName()).isEqualTo(StatusType.WATCHING);
                    assertThat(result.get(1).getName()).isEqualTo(StatusType.COMPLETED);
                    assertThat(result.get(2).getName()).isEqualTo(StatusType.PLAN_TO_WATCH);
                    assertThat(result.get(3).getName()).isEqualTo(StatusType.DROPPED);
                });
    }

    @Test
    void findAllMediaStatuses_WhenEmpty_ShouldReturnEmptyList() {
        // Given
        when(mediaStatusService.findAll()).thenReturn(List.of());

        // When & Then
        graphQlTester.document("""
                query {
                    findAllMediaStatuses {
                        id
                        name
                    }
                }
                """)
                .execute()
                .errors().verify()
                .path("findAllMediaStatuses").entityList(MediaStatusModel.class)
                .satisfies(result -> {
                    assertThat(result).isEmpty();
                });
    }

    @Test
    void findAllMediaStatuses_ShouldReturnCorrectIds() {
        // Given
        MediaStatusModel watching = new MediaStatusModel(1L, StatusType.WATCHING, null);
        MediaStatusModel completed = new MediaStatusModel(2L, StatusType.COMPLETED, null);

        when(mediaStatusService.findAll()).thenReturn(List.of(watching, completed));

        // When & Then
        graphQlTester.document("""
                query {
                    findAllMediaStatuses {
                        id
                        name
                    }
                }
                """)
                .execute()
                .errors().verify()
                .path("findAllMediaStatuses[0].id").entity(String.class).isEqualTo("1")
                .path("findAllMediaStatuses[0].name").entity(String.class).isEqualTo("WATCHING")
                .path("findAllMediaStatuses[1].id").entity(String.class).isEqualTo("2")
                .path("findAllMediaStatuses[1].name").entity(String.class).isEqualTo("COMPLETED");
    }
}
