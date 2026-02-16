package com.espacogeek.geek.data.api;

import java.util.List;

import lombok.Getter;
import org.json.simple.JSONArray;

import com.espacogeek.geek.models.AlternativeTitleModel;
import com.espacogeek.geek.models.ExternalReferenceModel;
import com.espacogeek.geek.models.GenreModel;
import com.espacogeek.geek.models.MediaCategoryModel;
import com.espacogeek.geek.models.MediaModel;
import com.espacogeek.geek.models.SeasonModel;

import info.movito.themoviedbapi.model.keywords.Keyword;

public interface MediaApi {
    @Getter
    enum ExternalCDN {
        TMDB("https://image.tmdb.org/t/p/original");

        private final String url;

        ExternalCDN(String url) {
            this.url = url;
        }
    }

    @Getter
    enum ApiKey {
        TMDB_API_KEY_ID(1),
        IGDB_CLIENT_ID(2),
        IGDB_TOKEN(3),
        IGDB_CLIENT_SECRET(4),
        NINJA_QUOTE_API_KEY(5);

        private final Integer id;

        ApiKey(Integer id) {
            this.id = id;
        }
    }

    default JSONArray updateTitles() {
        throw new UnsupportedOperationException();
    }

    default MediaModel getDetails(Integer id) {
        throw new UnsupportedOperationException();
    }

    default MediaModel getArtwork(Integer id) {
        throw new UnsupportedOperationException();
    }

    default MediaModel getUpdateBasicAttributes(Integer id) {
        throw new UnsupportedOperationException();
    }

    default List<Keyword> getKeyword(Integer id) {
        throw new UnsupportedOperationException();
    }

    default List<AlternativeTitleModel> getAlternativeTitles(Integer id) {
        throw new UnsupportedOperationException();
    }

    default List<ExternalReferenceModel> getExternalReference(Integer id) {
        throw new UnsupportedOperationException();
    }

    default List<GenreModel> getGenre(Integer id) {
        throw new UnsupportedOperationException();
    }

    default List<SeasonModel> getSeason(Integer id) {
        throw new UnsupportedOperationException();
    }

    default List<MediaModel> doSearch(String search) {
        throw new UnsupportedOperationException();
    }

    default List<MediaModel> doSearch(String search, MediaCategoryModel mediaCategoryModel) {
        throw new UnsupportedOperationException();
    }
}
