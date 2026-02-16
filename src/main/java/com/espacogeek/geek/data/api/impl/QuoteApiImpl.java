package com.espacogeek.geek.data.api.impl;

import java.io.IOException;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.espacogeek.geek.data.api.MediaApi;
import com.espacogeek.geek.data.api.QuoteApi;
import com.espacogeek.geek.exception.GenericException;
import com.espacogeek.geek.models.ApiKeyModel;
import com.espacogeek.geek.models.QuoteModel;
import com.espacogeek.geek.services.ApiKeyService;

import jakarta.annotation.PostConstruct;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Component("quoteApiImpl")
@Qualifier("quoteApiImpl")
@RequiredArgsConstructor
@Slf4j
public class QuoteApiImpl implements QuoteApi {
    private final ApiKeyService apiKeyService;
    private ApiKeyModel apiKey;
    private final static String URL_QUOTE = "https://api.api-ninjas.com/v1/quotes";

    @PostConstruct
    public void init() {
        Optional<ApiKeyModel> optionalApiKey = apiKeyService.findById(MediaApi.ApiKey.NINJA_QUOTE_API_KEY.getId());
        if (optionalApiKey.isPresent()) {
            apiKey = optionalApiKey.get();
        } else {
            log.error("API key for Ninja Quote is missing or blank.");
        }
    }

    @Override
    public QuoteModel getRandomQuote() {
        var client = new OkHttpClient().newBuilder().build();
        Request request = null;
        try {
            request = new Request.Builder()
                    .url(URL_QUOTE)
                    .method("GET", null)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-Api-Key", apiKey.getKey())
                    .build();
        } catch (Exception e) {
            log.error("Error building request for Quote API: {}", e.getMessage());
            throw new GenericException("Quote not found");
        }

        Response response = null;
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            log.error("Error executing request for Quote API: {}", e.getMessage());
            throw new GenericException("Quote not found");
        }

        var parser = new JSONParser();
        var jsonArray = new JSONArray();
        try {
            assert response.body() != null;
            jsonArray = (JSONArray) parser.parse(response.body().string());
        } catch (ParseException | IOException e) {
            log.error("Error parsing response from Quote API: {}", e.getMessage());
            throw new GenericException("Quote not found");
        }

        var jsonObject = (JSONObject) jsonArray.getFirst();

        return new QuoteModel(jsonObject.get("quote").toString(), jsonObject.get("author").toString());
    }

}
