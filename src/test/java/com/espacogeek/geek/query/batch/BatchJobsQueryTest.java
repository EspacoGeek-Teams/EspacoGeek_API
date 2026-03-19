package com.espacogeek.geek.query.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.espacogeek.geek.controllers.BatchJobController;
import com.espacogeek.geek.types.BatchJobPage;

/**
 * Unit tests for the {@code getBatchJobs} GraphQL query backed by {@link BatchJobController}.
 * <p>
 * The {@link JobExplorer} is mocked to avoid any Spring Batch datasource dependency.
 * Spring Security's {@code @PreAuthorize} is intentionally not tested here — it is
 * covered by the REST integration tests.
 */
@GraphQlTest(BatchJobController.class)
@ActiveProfiles("test")
public class BatchJobsQueryTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private JobExplorer jobExplorer;

    // ---- helpers ----

    private JobExecution stubExecution(long id, BatchStatus status,
            LocalDateTime start, LocalDateTime end) {
        JobExecution exec = mock(JobExecution.class);
        when(exec.getId()).thenReturn(id);
        when(exec.getStatus()).thenReturn(status);
        when(exec.getStartTime()).thenReturn(start);
        when(exec.getEndTime()).thenReturn(end);
        when(exec.getExitStatus()).thenReturn(new ExitStatus(status.name()));
        return exec;
    }

    // ---- tests ----

    @Test
    void getBatchJobs_NoJobs_ShouldReturnEmptyPage() {
        when(jobExplorer.getJobNames()).thenReturn(Collections.emptyList());

        graphQlTester.document("""
                query {
                    getBatchJobs(page: 0, size: 10) {
                        totalElements
                        totalPages
                        number
                        size
                        content {
                            id
                            jobName
                            status
                        }
                    }
                }
                """)
            .execute()
            .path("getBatchJobs")
            .entity(BatchJobPage.class)
            .satisfies(page -> {
                assertThat(page.getTotalElements()).isZero();
                assertThat(page.getTotalPages()).isZero();
                assertThat(page.getNumber()).isZero();
                assertThat(page.getSize()).isEqualTo(10);
                assertThat(page.getContent()).isEmpty();
            });
    }

    @Test
    void getBatchJobs_WithJobs_ShouldReturnPaginatedResults() {
        JobInstance instance = mock(JobInstance.class);
        LocalDateTime start = LocalDateTime.of(2024, 6, 1, 10, 0);
        LocalDateTime end   = LocalDateTime.of(2024, 6, 1, 10, 30);
        JobExecution exec = stubExecution(1L, BatchStatus.COMPLETED, start, end);

        when(jobExplorer.getJobNames()).thenReturn(List.of("movieJob"));
        when(jobExplorer.getJobInstances("movieJob", 0, 100)).thenReturn(List.of(instance));
        when(jobExplorer.getJobExecutions(instance)).thenReturn(List.of(exec));

        graphQlTester.document("""
                query {
                    getBatchJobs(page: 0, size: 10) {
                        totalElements
                        totalPages
                        number
                        size
                        content {
                            id
                            jobName
                            status
                            startTime
                            endTime
                            exitCode
                        }
                    }
                }
                """)
            .execute()
            .path("getBatchJobs.totalElements").entity(Integer.class).isEqualTo(1)
            .path("getBatchJobs.totalPages").entity(Integer.class).isEqualTo(1)
            .path("getBatchJobs.content[0].id").entity(String.class).isEqualTo("1")
            .path("getBatchJobs.content[0].jobName").entity(String.class).isEqualTo("movieJob")
            .path("getBatchJobs.content[0].status").entity(String.class).isEqualTo("COMPLETED")
            .path("getBatchJobs.content[0].exitCode").entity(String.class).isEqualTo("COMPLETED");
    }

    @Test
    void getBatchJobs_WithStatusFilter_ShouldReturnOnlyMatchingExecutions() {
        JobInstance instance = mock(JobInstance.class);
        LocalDateTime start = LocalDateTime.of(2024, 6, 1, 9, 0);
        JobExecution completedExec = stubExecution(1L, BatchStatus.COMPLETED, start, start.plusHours(1));
        JobExecution failedExec    = stubExecution(2L, BatchStatus.FAILED, start, null);

        when(jobExplorer.getJobNames()).thenReturn(List.of("movieJob"));
        when(jobExplorer.getJobInstances("movieJob", 0, 100)).thenReturn(List.of(instance));
        when(jobExplorer.getJobExecutions(instance)).thenReturn(List.of(completedExec, failedExec));

        graphQlTester.document("""
                query {
                    getBatchJobs(page: 0, size: 10, status: "FAILED") {
                        totalElements
                        content {
                            id
                            status
                        }
                    }
                }
                """)
            .execute()
            .path("getBatchJobs.totalElements").entity(Integer.class).isEqualTo(1)
            .path("getBatchJobs.content[0].id").entity(String.class).isEqualTo("2")
            .path("getBatchJobs.content[0].status").entity(String.class).isEqualTo("FAILED");
    }

    @Test
    void getBatchJobs_StatusFilterIsCaseInsensitive() {
        JobInstance instance = mock(JobInstance.class);
        LocalDateTime start = LocalDateTime.of(2024, 6, 1, 8, 0);
        JobExecution exec = stubExecution(5L, BatchStatus.STARTED, start, null);

        when(jobExplorer.getJobNames()).thenReturn(List.of("serieJob"));
        when(jobExplorer.getJobInstances("serieJob", 0, 100)).thenReturn(List.of(instance));
        when(jobExplorer.getJobExecutions(instance)).thenReturn(List.of(exec));

        // Use lowercase status in the filter
        graphQlTester.document("""
                query {
                    getBatchJobs(page: 0, size: 10, status: "started") {
                        totalElements
                        content {
                            id
                            status
                        }
                    }
                }
                """)
            .execute()
            .path("getBatchJobs.totalElements").entity(Integer.class).isEqualTo(1)
            .path("getBatchJobs.content[0].status").entity(String.class).isEqualTo("STARTED");
    }

    @Test
    void getBatchJobs_Pagination_ShouldSliceCorrectly() {
        JobInstance instance = mock(JobInstance.class);
        LocalDateTime t = LocalDateTime.of(2024, 1, 1, 0, 0);
        JobExecution exec1 = stubExecution(3L, BatchStatus.COMPLETED, t, t.plusMinutes(10));
        JobExecution exec2 = stubExecution(2L, BatchStatus.COMPLETED, t, t.plusMinutes(20));
        JobExecution exec3 = stubExecution(1L, BatchStatus.COMPLETED, t, t.plusMinutes(30));

        when(jobExplorer.getJobNames()).thenReturn(List.of("movieJob"));
        when(jobExplorer.getJobInstances("movieJob", 0, 100)).thenReturn(List.of(instance));
        when(jobExplorer.getJobExecutions(instance)).thenReturn(List.of(exec1, exec2, exec3));

        // Page 0 with size 2: should return the 2 most recent (highest IDs)
        graphQlTester.document("""
                query {
                    getBatchJobs(page: 0, size: 2) {
                        totalElements
                        totalPages
                        content {
                            id
                        }
                    }
                }
                """)
            .execute()
            .path("getBatchJobs.totalElements").entity(Integer.class).isEqualTo(3)
            .path("getBatchJobs.totalPages").entity(Integer.class).isEqualTo(2)
            .path("getBatchJobs.content[0].id").entity(String.class).isEqualTo("3")
            .path("getBatchJobs.content[1].id").entity(String.class).isEqualTo("2");
    }

    @Test
    void getBatchJobs_MultipleJobNames_ShouldAggregateAndSortByIdDesc() {
        JobInstance movieInstance = mock(JobInstance.class);
        JobInstance serieInstance = mock(JobInstance.class);
        LocalDateTime t = LocalDateTime.of(2024, 3, 1, 12, 0);
        JobExecution movieExec = stubExecution(10L, BatchStatus.COMPLETED, t, t.plusMinutes(5));
        JobExecution serieExec = stubExecution(20L, BatchStatus.FAILED, t, null);

        when(jobExplorer.getJobNames()).thenReturn(List.of("movieJob", "serieJob"));
        when(jobExplorer.getJobInstances("movieJob", 0, 100)).thenReturn(List.of(movieInstance));
        when(jobExplorer.getJobExecutions(movieInstance)).thenReturn(List.of(movieExec));
        when(jobExplorer.getJobInstances("serieJob", 0, 100)).thenReturn(List.of(serieInstance));
        when(jobExplorer.getJobExecutions(serieInstance)).thenReturn(List.of(serieExec));

        // Highest ID (serieExec=20) should come first
        graphQlTester.document("""
                query {
                    getBatchJobs(page: 0, size: 10) {
                        totalElements
                        content {
                            id
                            jobName
                        }
                    }
                }
                """)
            .execute()
            .path("getBatchJobs.totalElements").entity(Integer.class).isEqualTo(2)
            .path("getBatchJobs.content[0].id").entity(String.class).isEqualTo("20")
            .path("getBatchJobs.content[0].jobName").entity(String.class).isEqualTo("serieJob")
            .path("getBatchJobs.content[1].id").entity(String.class).isEqualTo("10")
            .path("getBatchJobs.content[1].jobName").entity(String.class).isEqualTo("movieJob");
    }
}
