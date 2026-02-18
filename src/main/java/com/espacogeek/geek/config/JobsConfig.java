package com.espacogeek.geek.config;

import com.espacogeek.geek.batch.*;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class JobsConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Step updateMoviesStep(
        MovieJsonReader movieJsonReader,
        MovieProcessor movieProcessor,
        MovieItemWriter movieItemWriter
    ) {
        return new StepBuilder("updateMoviesStep", jobRepository)
            .<org.json.simple.JSONObject, com.espacogeek.geek.models.MediaModel>chunk(10, transactionManager)
            .reader(movieJsonReader)
            .processor(movieProcessor)
            .writer(movieItemWriter)
            .listener(new DelayChunkListener(2000))
            .build();
    }

    @Bean
    public Job updateMoviesJob(Step updateMoviesStep) {
        return new JobBuilder("updateMoviesJob", jobRepository)
            .start(updateMoviesStep)
            .build();
    }

    @Bean
    public Step updateSeriesStep(
        SerieJsonReader serieJsonReader,
        SerieProcessor serieProcessor,
        SerieItemWriter serieItemWriter
    ) {
        return new StepBuilder("updateSeriesStep", jobRepository)
            .<org.json.simple.JSONObject, com.espacogeek.geek.models.MediaModel>chunk(10, transactionManager)
            .reader(serieJsonReader)
            .processor(serieProcessor)
            .writer(serieItemWriter)
            .listener(new DelayChunkListener(2000))
            .build();
    }

    @Bean
    public Job updateSeriesJob(Step updateSeriesStep) {
        return new JobBuilder("updateSeriesJob", jobRepository)
            .start(updateSeriesStep)
            .build();
    }
}
