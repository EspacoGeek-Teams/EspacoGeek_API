package com.espacogeek.geek.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Controller
@RestController
@RequestMapping("/admin/batch")
@RequiredArgsConstructor
public class BatchController {
    private final ApplicationContext ctx;
    private final JobOperator jobOperator;
    @Qualifier("asyncJobLauncher")
    private final JobLauncher asyncJobLauncher;

    @GetMapping("/run/{jobName}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<String> runJob(@PathVariable String jobName) {
        try {
            Job job = ctx.getBean(jobName, Job.class);
            JobParameters params = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

            JobExecution exec = asyncJobLauncher.run(job, params);

            Long executionId = exec.getId();
            return ResponseEntity.ok("Job ID: " + executionId);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro: " + e.getMessage());
        }
    }

    @GetMapping("/stop/{executionId}")
    @PreAuthorize("hasRole('aadmin')")
    public ResponseEntity<String> stopJob(@PathVariable Long executionId) {
        try {
            boolean result = jobOperator.stop(executionId);
            return ResponseEntity.ok("stop requested: " + result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("stop failed: " + e.getMessage());
        }
    }

    @GetMapping("/abandon/{executionId}")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<String> abandonJob(@PathVariable Long executionId) {
        try {
            jobOperator.abandon(executionId);
            return ResponseEntity.ok("abandoned");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("abandon failed: " + e.getMessage());
        }
    }
}
