package com.espacogeek.geek.data.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.espacogeek.geek.data.api.MediaApi;
import com.espacogeek.geek.exception.GenericException;
import com.espacogeek.geek.models.ExternalReferenceModel;
import com.espacogeek.geek.models.MediaCategoryModel;
import com.espacogeek.geek.models.MediaModel;
import com.espacogeek.geek.models.TypeReferenceModel;
import com.espacogeek.geek.services.ExternalReferenceService;
import com.espacogeek.geek.services.GenreService;
import com.espacogeek.geek.services.MediaService;
import com.espacogeek.geek.services.AlternativeTitlesService;
import com.espacogeek.geek.services.MediaCategoryService;
import com.espacogeek.geek.services.SeasonService;
import com.espacogeek.geek.services.TypeReferenceService;

import jakarta.annotation.PostConstruct;

@Component("movieController")
@Qualifier("movieController")
@Slf4j
public class MovieControllerImpl extends GenericMediaDataControllerImpl {
    private final MediaApi movieAPI;
    private final MediaCategoryService mediaCategoryService;
    private final ExternalReferenceService externalReferenceService;
    private final TypeReferenceService typeReferenceService;

    private TypeReferenceModel typeReference;

    @Autowired
    public MovieControllerImpl(
            MediaApi movieAPI,
            MediaCategoryService mediaCategoryService,
            ExternalReferenceService externalReferenceService,
            TypeReferenceService typeReferenceService,
            MediaService mediaService,
            GenreService genreService,
            AlternativeTitlesService alternativeTitlesService,
            ExternalReferenceService baseExternalReferenceService,
            SeasonService seasonService
    ) {
        super(mediaService, genreService, alternativeTitlesService, baseExternalReferenceService, seasonService);
        this.movieAPI = movieAPI;
        this.mediaCategoryService = mediaCategoryService;
        this.externalReferenceService = externalReferenceService;
        this.typeReferenceService = typeReferenceService;
    }

    @PostConstruct
    private void init() {
        this.typeReference = typeReferenceService.findById(ExternalReferenceType.TMDB.getId()).orElseThrow(() -> new GenericException("Type Reference not found"));
    }

    /**
     * This method update and add title of movie.
     * <p>
     * Every day at 10:00PM this function is executed.
     */
    private void updateMovies() {
        log.info("START TO UPDATE movie, AT {}", LocalDateTime.now());

        MediaCategoryModel mediaMovieCategory = mediaCategoryService.findById(MediaType.MOVIE.getId()).orElseThrow(() -> new GenericException("Category not found"));
        MediaCategoryModel mediaAnimeCategory = mediaCategoryService.findById(MediaType.ANIME_MOVIE.getId()).orElseThrow(() -> new GenericException("Category not found"));
        MediaCategoryModel mediaUndefinedCategory = mediaCategoryService.findById(MediaType.UNDEFINED_MEDIA.getId()).orElseThrow(() -> new GenericException("Category not found"));

        try(ExecutorService executorService = Executors.newFixedThreadPool(4)) {
            var jsonArrayDailyExport = movieAPI.updateTitles();
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

                        if (externalReferenceExisted.isEmpty()) {
                            boolean isAnime = false;
                            boolean isUndefined = false;
                            try {
                                isAnime = movieAPI.getKeyword(Integer.valueOf(json.get("id").toString())).stream().anyMatch((keyword) -> keyword.getName().equalsIgnoreCase("anime"));
                            } catch (Exception e) {
                                log.error("Error fetching keywords for movie ID {}: {}", json.get("id").toString(), e.getMessage());
                                isUndefined = true;
                            }

                            if (isAnime)
                                media.setMediaCategory(mediaAnimeCategory);
                            else if (isUndefined)
                                media.setMediaCategory(mediaUndefinedCategory);
                            else
                                media.setMediaCategory(mediaMovieCategory);

                            media.setName(json.get("original_title").toString());

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

                            media.setAlternativeTitles(updateAlternativeTitles(mediaSaved, null, typeReference, movieAPI));
                        }
                    } catch (Exception e) {
                        var json = (JSONObject) jsonArrayDailyExport.get(index);
                        log.error("Error processing movie {} - {}", json.get("id").toString(), json.get("original_name").toString(), e);
                    }
                });
            }
            executorService.shutdown();

            log.info("SUCCESS TO UPDATE movie, AT {}", LocalDateTime.now());
        } catch (Exception e) {
            log.error("FAILED TO UPDATE movie, AT {}", LocalDateTime.now(), e);
        }
    }

    @Override
    public MediaModel updateAllInformation(MediaModel media, MediaModel result) {
        return super.updateAllInformation(media, result, this.typeReference, this.movieAPI);
    }

    // Spring Shell
    public void updateMoviesNow() {
        updateMovies();
    }
}
