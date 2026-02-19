package com.espacogeek.geek.batch;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.scope.context.ChunkContext;

@Slf4j
public class DelayChunkListener implements ChunkListener {
    private final long delayInMillis;

    public DelayChunkListener(long delayInMillis) {
        this.delayInMillis = delayInMillis;
    }

    @Override
    public void afterChunk(@NotNull ChunkContext context) {
        try {
            log.debug("Chunk completed successfully. Applying delay of {}ms...", delayInMillis);
            Thread.sleep(delayInMillis);
        } catch (InterruptedException e) {
            log.error("Delay thread interrupted", e);
            Thread.currentThread().interrupt();
        }
    }
}
