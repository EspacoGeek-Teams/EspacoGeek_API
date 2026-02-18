package com.espacogeek.geek.batch;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.espacogeek.geek.exception.GenericException;
import com.espacogeek.geek.data.api.MediaApi;
import com.espacogeek.geek.models.ExternalReferenceModel;
import com.espacogeek.geek.models.TypeReferenceModel;
import com.espacogeek.geek.models.MediaModel;
import com.espacogeek.geek.models.MediaCategoryModel;
import com.espacogeek.geek.services.ExternalReferenceService;
import com.espacogeek.geek.services.MediaCategoryService;
import com.espacogeek.geek.services.TypeReferenceService;
import com.espacogeek.geek.data.MediaDataController.ExternalReferenceType;
import com.espacogeek.geek.data.MediaDataController.MediaType;

@Component
@StepScope
@RequiredArgsConstructor
@Slf4j
public class SerieProcessor implements ItemProcessor<JSONObject, MediaModel> {
    @Qualifier("tvSeriesApi")
    private final MediaApi tvSeriesApi;
    private final MediaCategoryService mediaCategoryService;
    private final ExternalReferenceService externalReferenceService;
    private final TypeReferenceService typeReferenceService;

    private TypeReferenceModel typeReference;
    private MediaCategoryModel mediaSerieCategory;
    private MediaCategoryModel mediaAnimeCategory;
    private MediaCategoryModel mediaUndefinedCategory;

    @PostConstruct
    public void init() {
        this.typeReference = typeReferenceService.findById(ExternalReferenceType.TMDB.getId())
                .orElseThrow(() -> new GenericException("Type Reference not found"));

        this.mediaSerieCategory = mediaCategoryService.findById(MediaType.SERIE.getId())
                .orElseThrow(() -> new GenericException("Category not found"));
        this.mediaAnimeCategory = mediaCategoryService.findById(MediaType.ANIME_SERIE.getId())
                .orElseThrow(() -> new GenericException("Category not found"));
        this.mediaUndefinedCategory = mediaCategoryService.findById(MediaType.UNDEFINED_MEDIA.getId())
                .orElseThrow(() -> new GenericException("Category not found"));
    }

    @Override
    public MediaModel process(JSONObject json) {
        if (json == null) return null;

        String idStr = json.get("id").toString();

        var existing = externalReferenceService.findByReferenceAndType(idStr, typeReference);
        if (existing.isPresent()) {
            // already exists, skip
            return null;
        }

        boolean isAnime = false;
        boolean isUndefined = false;
        try {
            isAnime = tvSeriesApi.getKeyword(Integer.valueOf(idStr)).stream()
                    .anyMatch(k -> k.getName().equalsIgnoreCase("anime"));
        } catch (Exception e) {
            log.error("Error fetching keywords for series ID {}: {}", idStr, e.getMessage());
            isUndefined = true;
        }

        MediaModel media = new MediaModel();
        if (isAnime) media.setMediaCategory(mediaAnimeCategory);
        else if (isUndefined) media.setMediaCategory(mediaUndefinedCategory);
        else media.setMediaCategory(mediaSerieCategory);

        var name = json.get("original_name") != null ? json.get("original_name").toString() : null;
        media.setName(name);

        ExternalReferenceModel externalReference = new ExternalReferenceModel();
        externalReference.setTypeReference(typeReference);
        externalReference.setReference(idStr);

        List<ExternalReferenceModel> refs = new ArrayList<>();
        refs.add(externalReference);
        media.setExternalReference(refs);

        return media;
    }
}
