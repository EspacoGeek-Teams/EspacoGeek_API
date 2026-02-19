package com.espacogeek.geek.data.api.impl;

import java.util.ArrayList;
import java.util.List;
import java.io.InputStream;

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
import com.espacogeek.geek.services.ApiKeyService;
import com.espacogeek.geek.services.GenreService;
import com.espacogeek.geek.services.MediaCategoryService;
import com.espacogeek.geek.services.TypeReferenceService;
import com.espacogeek.geek.utils.DataJumpUtils;
import com.espacogeek.geek.utils.DataJumpUtils.DataJumpTypeTMDB;

import info.movito.themoviedbapi.TmdbApi;
import info.movito.themoviedbapi.TmdbMovies;
import info.movito.themoviedbapi.model.core.AlternativeTitle;
import info.movito.themoviedbapi.model.core.Genre;
import info.movito.themoviedbapi.model.keywords.Keyword;
import info.movito.themoviedbapi.model.movies.ExternalIds;
import info.movito.themoviedbapi.model.movies.Images;
import info.movito.themoviedbapi.model.movies.MovieDb;
import info.movito.themoviedbapi.tools.TmdbException;
import info.movito.themoviedbapi.tools.appendtoresponse.MovieAppendToResponse;
import jakarta.annotation.PostConstruct;

import static com.espacogeek.geek.data.api.MediaApi.ApiKey.TMDB_API_KEY_ID;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@Component("movieAPI")
@Slf4j
@RequiredArgsConstructor
public class MovieAPIImpl implements MediaApi {
    private TmdbMovies api;

    private final ApiKeyService apiKeyService;
    private final TypeReferenceService typeReferenceService;
    private final MediaCategoryService mediaCategoryService;
    private final GenreService genreService;

    @PostConstruct
    private void init() {
        this.api = new TmdbApi(this.apiKeyService.findById(TMDB_API_KEY_ID.getId()).get().getKey()).getMovies();
    }

    /**
     * @see MediaApi#updateTitles()
     * @see DataJumpUtils#getDataJumpTMDBArray(DataJumpTypeTMDB)
     */
    @Override
    @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 2000), retryFor = com.espacogeek.geek.exception.RequestException.class)
    public JSONArray updateTitles() {
        return DataJumpUtils.getDataJumpTMDBArray(DataJumpTypeTMDB.MOVIE);
    }

    @Override
    @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 2000), retryFor = com.espacogeek.geek.exception.RequestException.class)
    public InputStream updateTitlesStream() {
        return DataJumpUtils.getDataJumpTMDBStream(DataJumpTypeTMDB.MOVIE);
    }

        /**
     * @see MediaApi#updateTitles(
     */
    @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 2000), retryFor = com.espacogeek.geek.exception.RequestException.class)
    @Override
    public MediaModel getDetails(Integer id) {
        MovieDb movieDb = new MovieDb();
        try {
            movieDb = api.getDetails(id, "en-US", MovieAppendToResponse.EXTERNAL_IDS, MovieAppendToResponse.ALTERNATIVE_TITLES, MovieAppendToResponse.IMAGES, MovieAppendToResponse.VIDEOS);
        } catch (TmdbException e) {
            log.error("Error fetching movie details", e);
            throw new com.espacogeek.geek.exception.RequestException();
        }

        var trailer = getTrailer(movieDb);
        var externalReferences = formatExternalReference(movieDb.getExternalIds(), movieDb.getId());

        if (trailer != null) externalReferences.add(trailer);

        MediaModel serie = new MediaModel(
                null,
                movieDb.getTitle(),
                null,
                movieDb.getRuntime(),
                movieDb.getOverview(),
                movieDb.getPosterPath() == null ? null : ExternalCDN.TMDB.getUrl() + movieDb.getPosterPath(),
                movieDb.getBackdropPath() == null ? null : ExternalCDN.TMDB.getUrl() + movieDb.getBackdropPath(),
                mediaCategoryService.findById(MediaDataController.MediaType.MOVIE.getId()).get(),
                externalReferences,
                null,
                null,
                formatGenre(movieDb.getGenres()),
                null,
                formatAlternativeTitles(movieDb.getAlternativeTitles().getTitles()),
                null);

        return serie;
    }

    public ExternalReferenceModel getTrailer(MovieDb movieDb) {
        ExternalReferenceModel trailers = null;

        trailers = movieDb.getVideos().getResults().stream().filter(video -> video.getType().equals("Trailer"))
                .findFirst().map(video -> new ExternalReferenceModel(null, video.getKey(), null, typeReferenceService.findById(MediaDataController.ExternalReferenceType.YT.getId()).get()))
                .orElse(null);

        return trailers;
    }

    /**
     * @see MediaApi#getArtwork(Integer)
     */
    @Override
    public MediaModel getArtwork(Integer id) {
        Images rawArtwork = new Images();
        try {
            rawArtwork = api.getImages(id, "en");
        } catch (TmdbException e) {
            log.error("Error fetching movie artwork", e);
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
            return api.getKeywords(id).getKeywords();
        } catch (TmdbException e) {
            log.error("Error fetching movie keywords", e);
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
            rawAlternativeTitles = api.getAlternativeTitles(id, "us").getTitles();
        } catch (TmdbException e) {
            log.error("Error fetching movie alternative titles", e);
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
            log.error("Error fetching movie external references", e);
            throw new com.espacogeek.geek.exception.RequestException();
        }
        return formatExternalReference(rawExternalReferences, id);
    }

    private List<ExternalReferenceModel> formatExternalReference(ExternalIds rawExternalReferences, Integer id) {
        var externalReferences = new ArrayList<ExternalReferenceModel>();

        externalReferences.add(new ExternalReferenceModel(null, id.toString(), null, typeReferenceService.findById(MediaDataController.ExternalReferenceType.TMDB.getId()).get()));

        if (rawExternalReferences != null && rawExternalReferences.getImdbId() != null) {
            externalReferences.add(new ExternalReferenceModel(null, rawExternalReferences.getImdbId(), null, typeReferenceService.findById(MediaDataController.ExternalReferenceType.IMDB.getId()).get()));
        }

        return externalReferences;
    }

    /**
     * @see MediaApi#getGenre(Integer)
     */
    @Override
    @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 2000), retryFor = com.espacogeek.geek.exception.RequestException.class)
    public List<GenreModel> getGenre(Integer id) {
        MovieDb movieDb = new MovieDb();
        try {
            movieDb = api.getDetails(id, "en-US");
        } catch (TmdbException e) {
            log.error("Error fetching movie genres", e);
            throw new com.espacogeek.geek.exception.RequestException();
        }

        if (movieDb == null || movieDb.getGenres() == null) {
            return new ArrayList<GenreModel>();
        }
        return formatGenre(movieDb.getGenres());
    }

    private List<GenreModel> formatGenre(List<Genre> rawGenres) {
        List<GenreModel> genres = new ArrayList<GenreModel>();
        List<String> rawStringGenres = rawGenres.stream().map((rawGenre) -> rawGenre.getName()).toList();
        List<String> newRawGenres = new ArrayList<String>();

        for (int i = 0; i < rawStringGenres.size(); i++) {
            var genre = rawStringGenres.get(i);
            if (genre.contains("&")) {
                for (String genreDivided : genre.split("&")) {
                    genreDivided.replace("&", "");
                    genreDivided = genreDivided.strip();
                    newRawGenres.add(genreDivided);
                }
            }
        }

        genres = genreService.findAllByNames(newRawGenres);

        return genres;
    }
}
