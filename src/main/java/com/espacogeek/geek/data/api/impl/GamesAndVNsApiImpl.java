package com.espacogeek.geek.data.api.impl;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.api.igdb.apicalypse.APICalypse;
import com.api.igdb.exceptions.RequestException;
import com.api.igdb.request.IGDBWrapper;
import com.api.igdb.request.ProtoRequestKt;
import com.api.igdb.request.TwitchAuthenticator;
import com.api.igdb.utils.ImageBuilderKt;
import com.api.igdb.utils.ImageSize;
import com.api.igdb.utils.ImageType;
import com.espacogeek.geek.data.MediaDataController;
import com.espacogeek.geek.data.api.MediaApi;
import com.espacogeek.geek.models.AlternativeTitleModel;
import com.espacogeek.geek.models.ExternalReferenceModel;
import com.espacogeek.geek.models.MediaCategoryModel;
import com.espacogeek.geek.models.MediaModel;
import com.espacogeek.geek.models.TypeReferenceModel;
import com.espacogeek.geek.services.ApiKeyService;
import com.espacogeek.geek.services.GenreService;
import com.espacogeek.geek.services.MediaCategoryService;
import com.espacogeek.geek.services.TypeReferenceService;

import jakarta.annotation.PostConstruct;
import proto.Game;
import proto.Search;

import static com.espacogeek.geek.data.api.MediaApi.ApiKey.*;

@Component("gamesAndVNsAPI")
@RequiredArgsConstructor
@Slf4j
public class GamesAndVNsApiImpl implements MediaApi {
    private final ApiKeyService apiKeyService;
    private IGDBWrapper wrapper;
    private TypeReferenceModel typeReference;
    private final TypeReferenceService typeReferenceService;
    private final MediaCategoryService mediaCategoryService;
    private MediaCategoryModel category;
    private final static String VN_ID_IGDB = "34"; // VN Genre ID in IGDB
    private final GenreService genreService;

    private void newToken() {
        var tAuth = TwitchAuthenticator.INSTANCE;
        var clientId = apiKeyService.findById(IGDB_CLIENT_ID.getId()).orElseThrow().getKey();
        var clientSecrete = apiKeyService.findById(IGDB_CLIENT_SECRET.getId()).orElseThrow().getKey();
        var token = tAuth.requestTwitchToken(clientId, clientSecrete);
        var tokenId = apiKeyService.findById(IGDB_TOKEN.getId()).orElseThrow();

        assert token != null;
        if (!token.getAccess_token().equals(tokenId.getKey())) {
            tokenId.setKey(token.getAccess_token());
            apiKeyService.save(tokenId);
        }

        wrapper.setCredentials(clientId, tokenId.getKey());
    }

    @PostConstruct
    private void init() {
        var tokenId = apiKeyService.findById(IGDB_TOKEN.getId()).orElseThrow();
        var clientId = apiKeyService.findById(IGDB_CLIENT_ID.getId()).orElseThrow().getKey();

        wrapper = IGDBWrapper.INSTANCE;
        wrapper.setCredentials(clientId, tokenId.getKey());

        typeReference = typeReferenceService.findById(MediaDataController.ExternalReferenceType.IGDB.getId()).orElseThrow();
        category = mediaCategoryService.findById(MediaDataController.MediaType.GAME.getId()).orElseThrow();
    }

    @Override
    @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 2000), retryFor = com.espacogeek.geek.exception.RequestException.class)
    public MediaModel getDetails(Integer id) {
        var apicalypse = new APICalypse().fields("*, artworks.image_id, cover.image_id, genres.name").where("id = " + id);
        MediaModel media = null;

        try {
            var searchGames = ProtoRequestKt.games(wrapper, apicalypse);

            for (Game result : searchGames) {
                if ((long) result.getId() != (long) 0L) {
                    var reference = new ExternalReferenceModel(null, String.valueOf(result.getId()), media, typeReference);
                    media = new MediaModel();

                    List<String> genresName = new ArrayList<>();
                    result.getGenresList().forEach((genre) -> {
                        genresName.add(genre.getName());
                    });

                    media.setGenre(genreService.findAllByNames(genresName));
                    media.setAbout(result.getSummary());
                    media.setName(result.getName());

                    media.setCover(
                        !result.getCover().getImageId().isEmpty()
                                    ? ImageBuilderKt.imageBuilder(result.getCover().getImageId(),
                                            ImageSize.COVER_BIG, ImageType.PNG)
                                    : null);
                    media.setBanner(result.getArtworksList().isEmpty() ? null
                            : ImageBuilderKt.imageBuilder(result.getArtworksList().getFirst().getImageId(),
                                    ImageSize.SCREENSHOT_HUGE, ImageType.PNG));

                    var alternativeTitles = new ArrayList<AlternativeTitleModel>();
                    for (proto.AlternativeName title : result.getAlternativeNamesList()) {
                        if (!title.getName().isEmpty()) alternativeTitles.add(new AlternativeTitleModel(null, title.getName(), media));
                    }
                    media.setAlternativeTitles(alternativeTitles);
                    media.setExternalReference(new ArrayList<>(List.of(reference)));
                    media.setMediaCategory(category);
                }
            }

        } catch (RequestException e) {
            newToken();
            log.error("Error while fetching details for media with id {}: {}", id, e.getMessage());
            throw new com.espacogeek.geek.exception.RequestException();
        }
        return media;
    }

    @Override
    @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 2000), retryFor = com.espacogeek.geek.exception.RequestException.class)
    public List<MediaModel> doSearch(String search, MediaCategoryModel mediaCategoryModel) {
        var apicalypse = new APICalypse().search(search).fields("game.age_ratings, game.aggregated_rating, game.alternative_names.name, game.artworks.image_id, game.cover.image_id, game.name").where("game.genres " + (mediaCategoryModel.getId() == MediaDataController.MediaType.GAME.getId() ? "!=" : "=") + " [" + VN_ID_IGDB + "]").limit(10);
        List<MediaModel> medias = new ArrayList<>();

        try {
            var searchGames = ProtoRequestKt.search(wrapper, apicalypse);

            for (Search result : searchGames) {
                if ((long) result.getGame().getId() != (long) 0L) {
                    var media = new MediaModel();
                    var reference = new ExternalReferenceModel(null, String.valueOf(result.getGame().getId()), media, typeReference);

                    media.setName(result.getGame().getName());
                    media.setCover(
                        !result.getGame().getCover().getImageId().isEmpty()
                                    ? ImageBuilderKt.imageBuilder(result.getGame().getCover().getImageId(),
                                            ImageSize.COVER_BIG, ImageType.PNG)
                                    : null);
                    media.setBanner(result.getGame().getArtworksList().isEmpty() ? null
                            : ImageBuilderKt.imageBuilder(result.getGame().getArtworksList().getFirst().getImageId(),
                                    ImageSize.SCREENSHOT_HUGE, ImageType.PNG));

                    var alternativeTitles = new ArrayList<AlternativeTitleModel>();
                    for (proto.AlternativeName title : result.getGame().getAlternativeNamesList()) {
                        if (!title.getName().isEmpty()) alternativeTitles.add(new AlternativeTitleModel(null, title.getName(), media));
                    }
                    media.setAlternativeTitles(alternativeTitles);
                    media.setExternalReference(new ArrayList<>(List.of(reference)));

                    media.setMediaCategory(category);

                    medias.add(media);
                }
            }

        } catch (RequestException e) {
            newToken();
            log.error("Error while searching for media with query {}: {}", search, e.getMessage());
            throw new com.espacogeek.geek.exception.RequestException();
        }

        return medias;
    }

    @Override
    @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 2000), retryFor = com.espacogeek.geek.exception.RequestException.class)
    public MediaModel getArtwork(Integer id) {
        var apicalypse = new APICalypse().fields("*, artworks.image_id, cover.image_id").where("id = " + id);
        MediaModel media = null;

        try {
            var searchGames = ProtoRequestKt.games(wrapper, apicalypse);

            for (Game result : searchGames) {
                if ((long) result.getId() != (long) 0L) {
                    media = new MediaModel();

                    media.setCover(
                        !result.getCover().getImageId().isEmpty()
                                    ? ImageBuilderKt.imageBuilder(result.getCover().getImageId(),
                                            ImageSize.COVER_BIG, ImageType.PNG)
                                    : null);
                    media.setBanner(result.getArtworksList().isEmpty() ? null
                            : ImageBuilderKt.imageBuilder(result.getArtworksList().getFirst().getImageId(),
                                    ImageSize.SCREENSHOT_HUGE, ImageType.PNG));
                }
            }

        } catch (RequestException e) {
            newToken();
            log.error("Error while fetching artwork for media with id {}: {}", id, e.getMessage());
            throw new com.espacogeek.geek.exception.RequestException();
        }

        return media;
    }
}
