package com.espacogeek.geek.data.impl;

import java.time.LocalDateTime;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.espacogeek.geek.data.api.MediaApi;
import com.espacogeek.geek.exception.GenericException;
import com.espacogeek.geek.models.ExternalReferenceModel;
import com.espacogeek.geek.models.MediaCategoryModel;
import com.espacogeek.geek.models.MediaModel;
import com.espacogeek.geek.models.TypeReferenceModel;
import com.espacogeek.geek.services.ExternalReferenceService;
import com.espacogeek.geek.services.MediaCategoryService;
import com.espacogeek.geek.services.TypeReferenceService;

import jakarta.annotation.PostConstruct;

@Component("serieController")
@Qualifier("serieController")
public class SerieControllerImpl extends GenericMediaDataControllerImpl {
    @Autowired
    private MediaApi tvSeriesApi;
    @Autowired
    private MediaCategoryService mediaCategoryService;
    @Autowired
    private ExternalReferenceService externalReferenceService;
    @Autowired
    private TypeReferenceService typeReferenceService;

    private TypeReferenceModel typeReference;

    private static final Logger log = LoggerFactory.getLogger(SerieControllerImpl.class);

    @PostConstruct
    private void init() {
        this.typeReference = typeReferenceService.findById(TMDB_ID).orElseThrow(() -> new GenericException("Type Reference not found"));
    }

    /**
     * This method update and add title of TV Series.
     * <p>
     * Every day at 9:00AM this function is executed.
     */
    @Scheduled(cron = "* * 9 * * *")
    // @Scheduled(initialDelay = 1)
    private void updateTvSeries() {
        log.info("START TO UPDATE TV SERIES, AT {}", LocalDateTime.now());

        MediaCategoryModel mediaSerieCategory = mediaCategoryService.findById(SERIE_ID).orElseThrow(() -> new GenericException("Category not found"));
        MediaCategoryModel mediaAnimeCategory = mediaCategoryService.findById(ANIME_SERIE_ID).orElseThrow(() -> new GenericException("Category not found"));
        MediaCategoryModel mediaUndefinedCategory = mediaCategoryService.findById(UNDEFINED_MEDIA_ID).orElseThrow(() -> new GenericException("Category not found"));
        ExecutorService executorService = Executors.newFixedThreadPool(400);

        try {
            var jsonArrayDailyExport = tvSeriesApi.updateTitles();
            for (int i = 0; i < jsonArrayDailyExport.size(); i++) {
                final int index = i;
                executorService.submit(() -> {
                    try {
                        var media = new MediaModel();

                        var externalReference = new ExternalReferenceModel();
                        externalReference.setTypeReference(typeReference);

                        var json = (JSONObject) jsonArrayDailyExport.get(index);

                        externalReference.setReference(json.get("id").toString());
                        var externalReferenceExisted = externalReferenceService.findByReferenceAndType(externalReference.getReference(), typeReference);

                        if (!externalReferenceExisted.isPresent()) {
                            boolean isAnime = false;
                            boolean isUndefined = false;
                            try {
                                isAnime = tvSeriesApi.getKeyword(Integer.valueOf(json.get("id").toString())).stream().anyMatch((keyword) -> keyword.getName().equalsIgnoreCase("anime"));
                            } catch (Exception e) {
                                log.error("Error fetching keywords for TV series ID {}: {}", json.get("id").toString(), e.getMessage());
                                isUndefined = true;
                            }

                            if (isAnime) media.setMediaCategory(mediaAnimeCategory);
                            else if (isUndefined) media.setMediaCategory(mediaUndefinedCategory);
                            else media.setMediaCategory(mediaSerieCategory);

                            media.setName(json.get("original_name").toString());

                            if (externalReferenceExisted.isPresent()) {
                                media.setId(externalReferenceExisted.get().getMedia().getId());
                                externalReference.setId(externalReferenceExisted.get().getId());
                            }

                            var mediaSaved = mediaService.save(media);

                            externalReference.setMedia(mediaSaved);
                            var referenceSaved = externalReferenceService.save(externalReference);
                            List<ExternalReferenceModel> referenceListSaved = new ArrayList<>();
                            referenceListSaved.add(referenceSaved);
                            mediaSaved.setExternalReference(referenceListSaved);

                            media.setAlternativeTitles(updateAlternativeTitles(mediaSaved, null, typeReference, tvSeriesApi));
                        }
                    } catch (Exception e) {
                        var json = (JSONObject) jsonArrayDailyExport.get(index);
                        log.error("Error processing TV series {} - {}", json.get("id").toString(), json.get("original_name").toString(), e);
                    }
                });
            }
            executorService.shutdown();
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

            log.info("SUCCESS TO UPDATE TV SERIES, AT {}", LocalDateTime.now());
        } catch (Exception e) {
            log.error("FAILED TO UPDATE TV SERIES, AT {}", LocalDateTime.now(), e);
        }
    }

    @Override
    public MediaModel updateAllInformation(MediaModel media, MediaModel result) {
        return super.updateAllInformation(media, result, this.typeReference, this.tvSeriesApi);
    }

    @Override
    public MediaModel updateArtworks(MediaModel media, MediaModel result) {
        return super.updateArtworks(media, result, this.typeReference, this.tvSeriesApi);
    }
}
