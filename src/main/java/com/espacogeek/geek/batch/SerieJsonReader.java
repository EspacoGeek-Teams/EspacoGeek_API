package com.espacogeek.geek.batch;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.support.AbstractItemStreamItemReader;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import com.espacogeek.geek.data.api.MediaApi;
import lombok.RequiredArgsConstructor;

/**
 * ItemReader that reads JSONObjects from the JSONArray returned by the TV Series MediaApi.
 */
@StepScope
@Component
@RequiredArgsConstructor
public class SerieJsonReader extends AbstractItemStreamItemReader<JSONObject> {
    @Qualifier("tvSeriesApi")
    private final MediaApi tvSeriesApi;

    private static final String KEY_NEXT_INDEX = "serieJsonReader.nextIndex";

    private JSONArray series = new JSONArray();
    private int nextIndex = 0;

    @Override
    public void open(ExecutionContext executionContext) {
        if (executionContext.containsKey(KEY_NEXT_INDEX)) {
            this.nextIndex = executionContext.getInt(KEY_NEXT_INDEX);
        } else {
            this.nextIndex = 0;
        }

        try {
            var result = tvSeriesApi.updateTitles();
            if (result != null) {
                this.series = result;
            }
        } catch (Exception e) {
            this.series = new JSONArray();
        }
    }

    @Override
    public void update(ExecutionContext executionContext) {
        executionContext.putInt(KEY_NEXT_INDEX, this.nextIndex);
    }

    @Override
    public JSONObject read() {
        if (series == null || nextIndex >= series.size()) {
            return null;
        }
        return (JSONObject) series.get(nextIndex++);
    }
}
