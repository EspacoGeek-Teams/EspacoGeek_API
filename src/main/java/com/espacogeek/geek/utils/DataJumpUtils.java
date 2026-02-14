package com.espacogeek.geek.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.zip.GZIPInputStream;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import jakarta.validation.constraints.NotNull;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class DataJumpUtils {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DataJumpUtils.class);

    public static enum DataJumpTypeTMDB {
        MOVIE("movie"), SERIES("tv_series"), PERSON("person");

        private final String value;

        DataJumpTypeTMDB(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     *
     * This function get the daily datajump available by tmdb
     *
     * @return a JSON Array with all serie titles
     * @throws IOException
     * @throws ParseException
     */
    @SuppressWarnings({ "unchecked", "null" })
    @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 2000), retryFor = com.espacogeek.geek.exception.RequestException.class)
    public static JSONArray getDataJumpTMDBArray(final @NotNull DataJumpTypeTMDB type) {
        var now = LocalDateTime.now();

        // formatting the date to do request as tmdb pattern
        var month = String.valueOf(now.getMonth().getValue()).length() == 1 ? "0".concat(String.valueOf(now.getMonth().getValue())) : now.getMonth().getValue();
        var day = String.valueOf(now.getDayOfMonth()).length() == 1 ? "0".concat(String.valueOf(now.getDayOfMonth())) : now.getDayOfMonth();
        var year = String.valueOf(now.getYear()).replace(".", "");

        var client = new OkHttpClient().newBuilder().build();
        Request request = null;
        try {
            request = new Request.Builder()
                    .url(MessageFormat.format("http://files.tmdb.org/p/exports/{3}_ids_{0}_{1}_{2}.json.gz", month, day, year, type.getValue()))
                    .method("GET", null)
                    .addHeader("Content-Type", "application/json")
                    .build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new com.espacogeek.geek.exception.RequestException();
        }

        Response response = null;
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

        GZIPInputStream inputStream = null;
        String[] json = null;
        try {
            inputStream = new GZIPInputStream(new ByteArrayInputStream(response.body().bytes()));
            json = new String(inputStream.readAllBytes()).split("\n");
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

        JSONArray jsonArray = new JSONArray();
        for (var item : json) {
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = null;
            try {
                jsonObject = (JSONObject) parser.parse(item);
            } catch (ParseException e) {
                log.error(e.getMessage(), e);
            }
            jsonArray.add(jsonObject);
        }
        return jsonArray;
    }
}
