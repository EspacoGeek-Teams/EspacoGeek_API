package com.espacogeek.geek.batch;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.support.AbstractItemStreamItemReader;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import com.espacogeek.geek.data.api.MediaApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * ItemReader that reads JSONObjects from the JSONArray returned by the TV Series MediaApi.
 */
@StepScope
@Component
@RequiredArgsConstructor
@Slf4j
public class SerieJsonReader extends AbstractItemStreamItemReader<JSONObject> {
    @Qualifier("tvSeriesApi")
    private final MediaApi tvSeriesApi;

    private static final String KEY_NEXT_INDEX = "serieJsonReader.nextIndex";

    private BufferedReader reader;
    private int currentLine = 0;

    @Override
    public void open(ExecutionContext executionContext) {
        int nextIndex = 0;
        if (executionContext.containsKey(KEY_NEXT_INDEX)) {
            nextIndex = executionContext.getInt(KEY_NEXT_INDEX);
        }

        try {
            InputStream is = tvSeriesApi.updateTitlesStream();
            this.reader = new BufferedReader(new InputStreamReader(is));
            // Skip already processed items
            for (int i = 0; i < nextIndex; i++) {
                if (reader.readLine() == null) break;
                currentLine++;
            }
        } catch (Exception e) {
            log.error("Error opening serie stream: {}", e.getMessage(), e);
        }
    }

    @Override
    public void update(ExecutionContext executionContext) {
        executionContext.putInt(KEY_NEXT_INDEX, this.currentLine);
    }

    @Override
    public JSONObject read() {
        if (reader == null) return null;
        try {
            String line = reader.readLine();
            if (line == null) return null;
            currentLine++;
            JSONParser parser = new JSONParser();
            return (JSONObject) parser.parse(line);
        } catch (Exception e) {
            log.error("Error reading/parsing serie JSON line {}: {}", currentLine, e.getMessage());
            return null;
        }
    }

    @Override
    public void close() {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                log.error("Error closing serie reader: {}", e.getMessage());
            }
        }
    }
}
