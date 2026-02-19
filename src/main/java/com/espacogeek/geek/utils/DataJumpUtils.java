package com.espacogeek.geek.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.zip.GZIPInputStream;

import lombok.Getter;
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

    @Getter
    public enum DataJumpTypeTMDB {
        MOVIE("movie"), SERIES("tv_series"), PERSON("person");

        private final String value;

        DataJumpTypeTMDB(String value) {
            this.value = value;
        }

    }

    /**
     *
     * This function get the daily datajump available by tmdb as a stream
     *
     * @return an uncompressed GZIP InputStream with all titles
     */
    public static InputStream getDataJumpTMDBStream(final @NotNull DataJumpTypeTMDB type) {
        var now = LocalDateTime.now();

        // formatting the date to do request as tmdb pattern
        var month = String.valueOf(now.getMonth().getValue()).length() == 1 ? "0".concat(String.valueOf(now.getMonth().getValue())) : now.getMonth().getValue();
        var day = String.valueOf(now.getDayOfMonth()).length() == 1 ? "0".concat(String.valueOf(now.getDayOfMonth())) : now.getDayOfMonth();
        var year = String.valueOf(now.getYear()).replace(".", "");

        var client = new OkHttpClient().newBuilder().build();
        Request request;
        try {
            request = new Request.Builder()
                    .url(MessageFormat.format("http://files.tmdb.org/p/exports/{3}_ids_{0}_{1}_{2}.json.gz", month, day, year, type.getValue()))
                    .method("GET", null)
                    .build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new com.espacogeek.geek.exception.RequestException();
        }

        Response response = null;
        try {
            response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                log.error("Failed to fetch TMDB daily export: {}", response.message());
                response.close();
                throw new com.espacogeek.geek.exception.RequestException();
            }
            assert response.body() != null;
            return new GZIPInputStream(response.body().byteStream());
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            if (response != null) response.close();
            throw new com.espacogeek.geek.exception.RequestException();
        }
    }

    /**
     *
     * This function get the daily datajump available by tmdb
     *
     * @return a JSON Array with all serie titles
     */
    @SuppressWarnings({ "unchecked" })
    @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 2000), retryFor = com.espacogeek.geek.exception.RequestException.class)
    public static JSONArray getDataJumpTMDBArray(final @NotNull DataJumpTypeTMDB type) {
        JSONArray jsonArray = new JSONArray();
        try (InputStream is = getDataJumpTMDBStream(type);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            JSONParser parser = new JSONParser();
            while ((line = reader.readLine()) != null) {
                try {
                    jsonArray.add(parser.parse(line));
                } catch (ParseException e) {
                    log.error("Error parsing TMDB JSON line: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error reading TMDB array: {}", e.getMessage());
        }
        return jsonArray;
    }
}
