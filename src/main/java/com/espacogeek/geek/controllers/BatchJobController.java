package com.espacogeek.geek.controllers;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.espacogeek.geek.types.BatchJobPage;

@Controller
@RequiredArgsConstructor
public class BatchJobController {
    private final JobExplorer jobExplorer;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Returns paginated Spring Batch job executions, optionally filtered by status.
     *
     * <p>Note: Spring Batch's {@link JobExplorer} does not expose native cross-job
     * pagination or status-based filtering at the query level, so all matching
     * executions are collected in memory before slicing. This is acceptable for
     * typical admin panel workloads where job history is bounded.
     *
     * @param page   Zero-based page number.
     * @param size   Number of items per page.
     * @param status Optional status filter (e.g. COMPLETED, FAILED, STARTED).
     * @return A {@link BatchJobPage} with the matching executions and pagination metadata.
     */
    @QueryMapping(name = "getBatchJobs")
    @PreAuthorize("hasRole('admin')")
    public BatchJobPage getBatchJobs(
            @Argument(name = "page") int page,
            @Argument(name = "size") int size,
            @Argument(name = "status") String status) {

        // Collect (numericId, dto) pairs so sorting parses the ID only once
        List<long[]> idSlots = new ArrayList<>();
        List<com.espacogeek.geek.types.BatchJobExecution> rawList = new ArrayList<>();

        for (String jobName : jobExplorer.getJobNames()) {
            int instanceStart = 0;
            int instanceBatchSize = 100;
            List<JobInstance> instances;
            do {
                instances = jobExplorer.getJobInstances(jobName, instanceStart, instanceBatchSize);
                for (JobInstance instance : instances) {
                    Collection<JobExecution> executions = jobExplorer.getJobExecutions(instance);
                    for (JobExecution exec : executions) {
                        String execStatus = exec.getStatus().name();
                        if (status != null && !status.isBlank() && !status.equalsIgnoreCase(execStatus)) {
                            continue;
                        }
                        com.espacogeek.geek.types.BatchJobExecution dto = new com.espacogeek.geek.types.BatchJobExecution();
                        dto.setId(String.valueOf(exec.getId()));
                        dto.setJobName(jobName);
                        dto.setStatus(execStatus);
                        dto.setStartTime(exec.getStartTime() != null ? exec.getStartTime().format(FORMATTER) : null);
                        dto.setEndTime(exec.getEndTime() != null ? exec.getEndTime().format(FORMATTER) : null);
                        dto.setExitCode(exec.getExitStatus() != null ? exec.getExitStatus().getExitCode() : null);
                        idSlots.add(new long[]{ exec.getId(), rawList.size() });
                        rawList.add(dto);
                    }
                }
                instanceStart += instanceBatchSize;
            } while (instances.size() == instanceBatchSize);
        }

        // Sort by numeric execution ID descending (most recent first); ID parsed once
        idSlots.sort((a, b) -> Long.compare(b[0], a[0]));

        List<com.espacogeek.geek.types.BatchJobExecution> allExecutions = new ArrayList<>(idSlots.size());
        for (long[] slot : idSlots) {
            allExecutions.add(rawList.get((int) slot[1]));
        }

        long totalElements = allExecutions.size();
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        int fromIndex = Math.min(page * size, (int) totalElements);
        int toIndex = Math.min(fromIndex + size, (int) totalElements);

        BatchJobPage result = new BatchJobPage();
        result.setContent(allExecutions.subList(fromIndex, toIndex));
        result.setTotalElements(totalElements);
        result.setTotalPages(totalPages);
        result.setNumber(page);
        result.setSize(size);
        return result;
    }
}
