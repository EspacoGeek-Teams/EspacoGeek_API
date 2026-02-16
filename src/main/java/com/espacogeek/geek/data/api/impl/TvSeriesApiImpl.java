package com.espacogeek.geek.data.api.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import info.movito.themoviedbapi.model.core.NamedIdElement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONArray;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.espacogeek.geek.data.MediaDataController;
import com.espacogeek.geek.data.api.MediaApi;
import com.espacogeek.geek.models.AlternativeTitleModel;
import com.espacogeek.geek.models.ExternalReferenceModel;
import com.espacogeek.geek.models.GenreModel;
import com.espacogeek.geek.models.MediaModel;
import com.espacogeek.geek.models.SeasonModel;
import com.espacogeek.geek.services.ApiKeyService;
import com.espacogeek.geek.services.GenreService;
import com.espacogeek.geek.services.MediaCategoryService;
import com.espacogeek.geek.services.TypeReferenceService;
import com.espacogeek.geek.utils.DataJumpUtils;
import com.espacogeek.geek.utils.DataJumpUtils.DataJumpTypeTMDB;

import info.movito.themoviedbapi.TmdbApi;
import info.movito.themoviedbapi.TmdbTvSeries;
import info.movito.themoviedbapi.model.core.AlternativeTitle;
import info.movito.themoviedbapi.model.core.Genre;
import info.movito.themoviedbapi.model.keywords.Keyword;
import info.movito.themoviedbapi.model.tv.core.TvSeason;
import info.movito.themoviedbapi.model.tv.series.ExternalIds;
import info.movito.themoviedbapi.model.tv.series.Images;
import info.movito.themoviedbapi.model.tv.series.TvSeriesDb;
import info.movito.themoviedbapi.tools.TmdbException;
import info.movito.themoviedbapi.tools.appendtoresponse.TvSeriesAppendToResponse;
import jakarta.annotation.PostConstruct;

import static com.espacogeek.geek.data.api.MediaApi.ApiKey.TMDB_API_KEY_ID;

@SuppressWarnings({"OptionalGetWithoutIsPresent", "DataFlowIssue"})
@Component("tvSeriesApi")
@RequiredArgsConstructor
@Slf4j
public class TvSeriesApiImpl implements MediaApi {
    private TmdbTvSeries api;

    private final ApiKeyService apiKeyService;
    private final MediaCategoryService mediaCategoryService;
    private final TypeReferenceService typeReferenceService;
    private final GenreService genreService;

    @PostConstruct
    private void init() {
        this.api = new TmdbApi(this.apiKeyService.findById(TMDB_API_KEY_ID.getId()).get().getKey()).getTvSeries();
    }

    /**
     * @see MediaApi#updateTitles()
     * @see DataJumpUtils#getDataJumpTMDBArray(DataJumpTypeTMDB)
     */
    @Override
    @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 2000), retryFor = com.espacogeek.geek.exception.RequestException.class)
    public JSONArray updateTitles() {
        return DataJumpUtils.getDataJumpTMDBArray(DataJumpTypeTMDB.SERIES);
    }

    /**
     * @see MediaApi#updateTitles()
     */
    @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 2000), retryFor = com.espacogeek.geek.exception.RequestException.class)
    @Override
    public MediaModel getDetails(Integer id) {
        TvSeriesDb rawSerieDetails = new TvSeriesDb();
        try {
            rawSerieDetails = api.getDetails(id, "en-US", TvSeriesAppendToResponse.EXTERNAL_IDS, TvSeriesAppendToResponse.ALTERNATIVE_TITLES, TvSeriesAppendToResponse.IMAGES, TvSeriesAppendToResponse.VIDEOS);
        } catch (TmdbException e) {
            log.error("Error fetching TV series details", e);
            throw new com.espacogeek.geek.exception.RequestException();
        }

        var season = formatSeason(rawSerieDetails.getSeasons());
        var trailer = getTrailer(rawSerieDetails);
        var externalReferences = formatExternalReference(rawSerieDetails.getExternalIds(), rawSerieDetails.getId());

        if (trailer != null) externalReferences.add(trailer);

        MediaModel serie = new MediaModel(
                null,
                rawSerieDetails.getName(),
                Optional.ofNullable(rawSerieDetails.getNumberOfEpisodes()).orElse(season.stream().map(SeasonModel::getEpisodeCount).reduce(Integer::sum).orElseGet(null)),
                rawSerieDetails.getEpisodeRunTime() == null || rawSerieDetails.getEpisodeRunTime().isEmpty() ? null : rawSerieDetails.getEpisodeRunTime().getFirst(),
                rawSerieDetails.getOverview(),
                rawSerieDetails.getPosterPath() == null ? null : ExternalCDN.TMDB.getUrl() + rawSerieDetails.getPosterPath(),
                rawSerieDetails.getBackdropPath() == null ? null : ExternalCDN.TMDB.getUrl() + rawSerieDetails.getBackdropPath(),
                mediaCategoryService.findById(MediaDataController.MediaType.SERIE.getId()).get(),
                externalReferences,
                null,
                null,
                formatGenre(rawSerieDetails.getGenres()),
                null,
                formatAlternativeTitles(rawSerieDetails.getAlternativeTitles().getResults()),
                season);

        return serie;
    }

    public ExternalReferenceModel getTrailer(TvSeriesDb rawSerieDetails) {
        ExternalReferenceModel trailers = null;

        trailers = rawSerieDetails.getVideos().getResults().stream().filter(video -> video.getType().equals("Trailer"))
                .findFirst().map(video -> new ExternalReferenceModel(null, video.getKey(), null,
                        typeReferenceService.findById(MediaDataController.ExternalReferenceType.YT.getId()).get()))
                .orElse(null);

        return trailers;
    }

    /**
     * @see MediaApi#getArtwork(Integer)
     */
    @Override
    @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 2000), retryFor = com.espacogeek.geek.exception.RequestException.class)
    public MediaModel getArtwork(Integer id) {
        Images rawArtwork = new Images();
        try {
            rawArtwork = api.getImages(id, "en");
        } catch (TmdbException e) {
            throw new com.espacogeek.geek.exception.RequestException();
        }
        var media = new MediaModel();

        media.setCover(rawArtwork.getPosters().isEmpty() ? "" : ExternalCDN.TMDB.getUrl() + rawArtwork.getPosters().getFirst());
        media.setBanner(rawArtwork.getBackdrops().isEmpty() ? "" : ExternalCDN.TMDB.getUrl() + rawArtwork.getBackdrops().getFirst());

        return media;
    }

    /**
     * @see MediaApi#getArtwork(Integer)
     */
    @Override
    @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 2000), retryFor = com.espacogeek.geek.exception.RequestException.class)
    public List<Keyword> getKeyword(Integer id) {
        try {
            return api.getKeywords(id).getResults();
        } catch (TmdbException e) {
            throw new com.espacogeek.geek.exception.RequestException();
        }
    }

    /**
     * @see MediaApi#getAlternativeTitles(Integer)
     */
    @Override
    @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 2000), retryFor = com.espacogeek.geek.exception.RequestException.class)
    public List<AlternativeTitleModel> getAlternativeTitles(Integer id) {
        List<AlternativeTitle> rawAlternativeTitles = new ArrayList<>();
        try {
            rawAlternativeTitles = api.getAlternativeTitles(id).getResults();
        } catch (TmdbException e) {
            throw new com.espacogeek.geek.exception.RequestException();
        }
        return formatAlternativeTitles(rawAlternativeTitles);
    }

    private List<AlternativeTitleModel> formatAlternativeTitles(List<AlternativeTitle> rawAlternativeTitles) {
        var alternativeTitles = new ArrayList<AlternativeTitleModel>();

        for (AlternativeTitle title : rawAlternativeTitles) {
            alternativeTitles.add(new AlternativeTitleModel(null, title.getTitle(), null));
        }

        return alternativeTitles;
    }

    /**
     * @see MediaApi#getExternalReference(Integer)
     */
    @Override
    @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 2000), retryFor = com.espacogeek.geek.exception.RequestException.class)
    public List<ExternalReferenceModel> getExternalReference(Integer id) {
        ExternalIds rawExternalReferences = new ExternalIds();
        try {
            rawExternalReferences = api.getExternalIds(id);
        } catch (TmdbException e) {
            throw new com.espacogeek.geek.exception.RequestException();
        }
        return formatExternalReference(rawExternalReferences, id);
    }

    private List<ExternalReferenceModel> formatExternalReference(ExternalIds rawExternalReferences, Integer id) {
        var externalReferences = new ArrayList<ExternalReferenceModel>();

        externalReferences.add(new ExternalReferenceModel(null, id.toString(), null,
                typeReferenceService.findById(MediaDataController.ExternalReferenceType.TMDB.getId()).get()));

        if (rawExternalReferences != null && rawExternalReferences.getTvdbId() != null) {
            externalReferences.add(new ExternalReferenceModel(null, rawExternalReferences.getTvdbId(), null,
                    typeReferenceService.findById(MediaDataController.ExternalReferenceType.TVDB.getId()).get()));
        }
        if (rawExternalReferences != null && rawExternalReferences.getImdbId() != null) {
            externalReferences.add(new ExternalReferenceModel(null, rawExternalReferences.getImdbId(), null,
                    typeReferenceService.findById(MediaDataController.ExternalReferenceType.IMDB.getId()).get()));
        }

        return externalReferences;
    }

    /**
     * @see MediaApi#getGenre(Integer)
     */
    @Override
    @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 2000), retryFor = com.espacogeek.geek.exception.RequestException.class)
    public List<GenreModel> getGenre(Integer id) {
        TvSeriesDb rawSerieDetails = new TvSeriesDb();
        try {
            rawSerieDetails = api.getDetails(id, "en-US");
        } catch (TmdbException e) {
            throw new com.espacogeek.geek.exception.RequestException();
        }

        if (rawSerieDetails == null || rawSerieDetails.getGenres() == null) {
            return new ArrayList<GenreModel>();
        }
        return formatGenre(rawSerieDetails.getGenres());
    }

    private List<GenreModel> formatGenre(List<Genre> rawGenres) {
        List<GenreModel> genres;
        List<String> rawStringGenres = rawGenres.stream().map(NamedIdElement::getName).toList();
        List<String> newRawGenres = new ArrayList<String>();

        for (String genre : rawStringGenres) {
            if (genre.contains("&")) {
                for (String genreDivided : genre.split("&")) {
                    genreDivided = genreDivided.replace("&", "");
                    genreDivided = genreDivided.strip();
                    newRawGenres.add(genreDivided);
                }
            }
        }

        genres = genreService.findAllByNames(newRawGenres);

        return genres;
    }

    /**
     * @see MediaApi#getSeason(Integer)
     */
    @Override
    @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 2000), retryFor = com.espacogeek.geek.exception.RequestException.class)
    public List<SeasonModel> getSeason(Integer id) {
        List<TvSeason> rawSession;

        try {
            rawSession = api.getDetails(id, "en-US").getSeasons();
        } catch (TmdbException e) {
            throw new com.espacogeek.geek.exception.RequestException();
        }

        return formatSeason(rawSession);
    }

    private List<SeasonModel> formatSeason(List<TvSeason> rawSeasons) {
        List<SeasonModel> seasons = new ArrayList<>();

        rawSeasons.forEach((rawSeason) -> {
            try {
                seasons.add(new SeasonModel(null, rawSeason.getName(),
                        rawSeason.getAirDate() == null ? null : new SimpleDateFormat("yyyy-MM-dd").parse(rawSeason.getAirDate()),
                        null,
                        rawSeason.getOverview(),
                        rawSeason.getPosterPath() == null ? null : ExternalCDN.TMDB.getUrl() + rawSeason.getPosterPath(),
                        rawSeason.getSeasonNumber(),
                        rawSeason.getEpisodeCount(),
                        null));
            } catch (java.text.ParseException e) {
                log.error("Error parsing season air date", e);
            }
        });

        return seasons;
    }
}
