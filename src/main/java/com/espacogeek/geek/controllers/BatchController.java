package com.espacogeek.geek.controllers;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Controller
@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
@Slf4j
public class BatchController {
    private final ApplicationContext ctx;
    private final JobOperator jobOperator;
    @Qualifier("asyncJobLauncher")
    private final JobLauncher asyncJobLauncher;

    /**
     * Start a batch job by name.
     * Accepts a JSON body with a {@code jobName} field, or falls back to a
     * {@code jobName} request parameter when no body is provided.
     *
     * @param body    Optional JSON body containing {@code jobName}.
     * @param jobName Optional request parameter fallback (used only when body is absent).
     * @return HTTP 200 with the new execution ID, or an appropriate error status.
     */
    @PostMapping("/start")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<String> startJob(
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam(name = "jobName", required = false) String jobName) {
        String name = (body != null && body.containsKey("jobName")) ? body.get("jobName") : jobName;
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body("jobName is required");
        }
        try {
            Job job = ctx.getBean(name, Job.class);
            JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

            JobExecution exec = asyncJobLauncher.run(job, params);
            return ResponseEntity.ok("Job ID: " + exec.getId());

        } catch (Exception e) {
            log.error("Failed to start job '{}': {}", name, e.getMessage(), e);
            return ResponseEntity.status(500).body("Failed to start job");
        }
    }

    /**
     * Gracefully stop a running job execution by ID.
     *
     * @param id The job execution ID.
     * @return HTTP 200 on success, 404 if not found, or 500 on error.
     */
    @PostMapping("/{id}/stop")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<String> stopJob(@PathVariable Long id) {
        try {
            boolean result = jobOperator.stop(id);
            return ResponseEntity.ok("stop requested: " + result);
        } catch (NoSuchJobExecutionException e) {
            return ResponseEntity.status(404).body("Execution not found: " + id);
        } catch (Exception e) {
            log.error("Failed to stop job execution {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(500).body("Failed to stop job execution");
        }
    }

    /**
     * Force abandon a stopped or failed job execution by ID.
     *
     * @param id The job execution ID.
     * @return HTTP 200 on success, 404 if not found, or 500 on error.
     */
    @PostMapping("/{id}/abandon")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<String> abandonJob(@PathVariable Long id) {
        try {
            jobOperator.abandon(id);
            return ResponseEntity.ok("abandoned");
        } catch (NoSuchJobExecutionException e) {
            return ResponseEntity.status(404).body("Execution not found: " + id);
        } catch (Exception e) {
            log.error("Failed to abandon job execution {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(500).body("Failed to abandon job execution");
        }
    }

    /**
     * Restart a failed or stopped job execution by ID.
     *
     * @param id The job execution ID.
     * @return HTTP 200 with the new execution ID, 404 if not found, 400 if already
     *         complete, or 500 on error.
     */
    @PostMapping("/{id}/restart")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<String> restartJob(@PathVariable Long id) {
        try {
            Long newExecutionId = jobOperator.restart(id);
            return ResponseEntity.ok("Job restarted! New Execution ID: " + newExecutionId);
        } catch (NoSuchJobExecutionException e) {
            return ResponseEntity.status(404).body("Execution not found: " + id);
        } catch (JobInstanceAlreadyCompleteException e) {
            return ResponseEntity.status(400).body("Job already completed successfully and cannot be restarted.");
        } catch (Exception e) {
            log.error("Failed to restart job execution {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(500).body("Failed to restart job execution");
        }
    }
}

