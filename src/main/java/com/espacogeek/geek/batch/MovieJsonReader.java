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
 * ItemReader that reads JSONObjects from the JSONArray returned by MediaApi.updateTitles().
 * Persists the current index in the Step ExecutionContext for restartability.
 */
@StepScope
@Component
@RequiredArgsConstructor
public class MovieJsonReader extends AbstractItemStreamItemReader<JSONObject> {
    @Qualifier("movieAPI")
    private final MediaApi movieApi;

    private static final String KEY_NEXT_INDEX = "movieJsonReader.nextIndex";

    private JSONArray movies = new JSONArray();
    private int nextIndex = 0;

    @Override
    public void open(ExecutionContext executionContext) {
        if (executionContext.containsKey(KEY_NEXT_INDEX)) {
            this.nextIndex = executionContext.getInt(KEY_NEXT_INDEX);
        } else {
            this.nextIndex = 0;
        }

        try {
            var result = movieApi.updateTitles();
            if (result != null) {
                this.movies = result;
            }
        } catch (Exception e) {
            this.movies = new JSONArray();
        }
    }

    @Override
    public void update(ExecutionContext executionContext) {
        executionContext.putInt(KEY_NEXT_INDEX, this.nextIndex);
    }

    @Override
    public JSONObject read() {
        if (movies == null || nextIndex >= movies.size()) {
            return null;
        }
        return (JSONObject) movies.get(nextIndex++);
    }
}
