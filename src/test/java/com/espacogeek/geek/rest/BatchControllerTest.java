package com.espacogeek.geek.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.espacogeek.geek.config.GraphQlCookieInterceptor;
import com.espacogeek.geek.config.JwtAuthenticationFilter;
import com.espacogeek.geek.config.JwtConfig;
import com.espacogeek.geek.config.SecurityConfig;
import com.espacogeek.geek.controllers.BatchController;
import com.espacogeek.geek.models.UserModel;
import com.espacogeek.geek.services.impl.UserDetailsServiceImpl;
import com.espacogeek.geek.utils.TokenUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Integration tests for {@link BatchController} REST endpoints at {@code /api/v1/batch/**}.
 * <p>
 * Verifies that:
 * <ul>
 *   <li>Each endpoint is reachable and returns the correct HTTP status on success.</li>
 *   <li>Error conditions (not found, already complete) map to the correct HTTP status codes.</li>
 *   <li>Unauthenticated requests are rejected with HTTP 403.</li>
 * </ul>
 * Spring Batch infrastructure beans ({@link JobOperator}, {@code asyncJobLauncher}) are
 * replaced by Mockito mocks, keeping the test fast and free of database dependencies.
 */
@SpringBootTest(
    classes = BatchControllerTest.TestConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.batch.jdbc.initialize-schema=embedded")
class BatchControllerTest {

    @SpringBootApplication
    @Import({
        BatchController.class,
        SecurityConfig.class,
        JwtConfig.class,
        JwtAuthenticationFilter.class,
        TokenUtils.class,
        GraphQlCookieInterceptor.class
    })
    static class TestConfig {
        /** Registers the custom Date scalar required by the GraphQL auto-configuration. */
        @Bean
        RuntimeWiringConfigurer dateScalarConfigurer() {
            return builder -> builder.scalar(com.espacogeek.geek.types.Scalars.dateType());
        }

        /** Provides a named Job bean so {@code ctx.getBean("testBatchJob", Job.class)} succeeds. */
        @Bean(name = "testBatchJob")
        public Job testBatchJob() {
            Job job = Mockito.mock(Job.class);
            Mockito.when(job.getName()).thenReturn("testBatchJob");
            return job;
        }
    }

    private static final String BASE = "/api/v1/batch";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtConfig jwtConfig;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @MockitoBean
    private JobOperator jobOperator;

    @MockitoBean(name = "asyncJobLauncher")
    private JobLauncher asyncJobLauncher;

    private String adminToken;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        UserModel adminUser = new UserModel();
        adminUser.setId(1);
        adminUser.setEmail("admin@test.com");
        adminUser.setUsername("admin");
        adminUser.setUserRole("ROLE_admin");
        adminToken = jwtConfig.generateToken(adminUser);
    }

    // ---- POST /api/v1/batch/start ----

    @Test
    void startJob_WithJsonBody_ShouldReturn200() throws Exception {
        JobExecution mockExec = mock(JobExecution.class);
        when(mockExec.getId()).thenReturn(42L);
        when(asyncJobLauncher.run(any(Job.class), any(JobParameters.class))).thenReturn(mockExec);

        String body = objectMapper.writeValueAsString(Map.of("jobName", "testBatchJob"));
        mockMvc.perform(post(BASE + "/start")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Job ID: 42")));
    }

    @Test
    void startJob_WithQueryParam_ShouldReturn200() throws Exception {
        JobExecution mockExec = mock(JobExecution.class);
        when(mockExec.getId()).thenReturn(7L);
        when(asyncJobLauncher.run(any(Job.class), any(JobParameters.class))).thenReturn(mockExec);

        mockMvc.perform(post(BASE + "/start")
                .param("jobName", "testBatchJob")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Job ID: 7")));
    }

    @Test
    void startJob_MissingJobName_ShouldReturn400() throws Exception {
        mockMvc.perform(post(BASE + "/start")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void startJob_Unauthenticated_ShouldBeRejected() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("jobName", "testBatchJob"));
        mockMvc.perform(post(BASE + "/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().is4xxClientError());
    }

    // ---- POST /api/v1/batch/{id}/stop ----

    @Test
    void stopJob_ShouldReturn200() throws Exception {
        when(jobOperator.stop(1L)).thenReturn(true);

        mockMvc.perform(post(BASE + "/1/stop")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("stop requested: true")));
    }

    @Test
    void stopJob_NotFound_ShouldReturn404() throws Exception {
        when(jobOperator.stop(99L)).thenThrow(new NoSuchJobExecutionException("not found"));

        mockMvc.perform(post(BASE + "/99/stop")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isNotFound());
    }

    @Test
    void stopJob_Unauthenticated_ShouldBeRejected() throws Exception {
        mockMvc.perform(post(BASE + "/1/stop"))
            .andExpect(status().is4xxClientError());
    }

    // ---- POST /api/v1/batch/{id}/abandon ----

    @Test
    void abandonJob_ShouldReturn200() throws Exception {
        when(jobOperator.abandon(1L)).thenReturn(mock(JobExecution.class));

        mockMvc.perform(post(BASE + "/1/abandon")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(content().string("abandoned"));
    }

    @Test
    void abandonJob_NotFound_ShouldReturn404() throws Exception {
        when(jobOperator.abandon(99L)).thenThrow(new NoSuchJobExecutionException("not found"));

        mockMvc.perform(post(BASE + "/99/abandon")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isNotFound());
    }

    @Test
    void abandonJob_Unauthenticated_ShouldBeRejected() throws Exception {
        mockMvc.perform(post(BASE + "/1/abandon"))
            .andExpect(status().is4xxClientError());
    }

    // ---- POST /api/v1/batch/{id}/restart ----

    @Test
    void restartJob_ShouldReturn200() throws Exception {
        when(jobOperator.restart(1L)).thenReturn(2L);

        mockMvc.perform(post(BASE + "/1/restart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("New Execution ID: 2")));
    }

    @Test
    void restartJob_NotFound_ShouldReturn404() throws Exception {
        when(jobOperator.restart(99L)).thenThrow(new NoSuchJobExecutionException("not found"));

        mockMvc.perform(post(BASE + "/99/restart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isNotFound());
    }

    @Test
    void restartJob_AlreadyComplete_ShouldReturn400() throws Exception {
        when(jobOperator.restart(1L))
            .thenThrow(new JobInstanceAlreadyCompleteException("already complete"));

        mockMvc.perform(post(BASE + "/1/restart")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
            .andExpect(status().isBadRequest());
    }

    @Test
    void restartJob_Unauthenticated_ShouldBeRejected() throws Exception {
        mockMvc.perform(post(BASE + "/1/restart"))
            .andExpect(status().is4xxClientError());
    }
}
