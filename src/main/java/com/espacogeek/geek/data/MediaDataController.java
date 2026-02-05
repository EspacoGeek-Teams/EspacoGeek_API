package com.espacogeek.geek.data;

import java.util.List;

import com.espacogeek.geek.data.api.MediaApi;
import com.espacogeek.geek.models.AlternativeTitleModel;
import com.espacogeek.geek.models.ExternalReferenceModel;
import com.espacogeek.geek.models.GenreModel;
import com.espacogeek.geek.models.MediaCategoryModel;
import com.espacogeek.geek.models.MediaModel;
import com.espacogeek.geek.models.SeasonModel;
import com.espacogeek.geek.models.TypeReferenceModel;

import jakarta.annotation.Nullable;

public interface MediaDataController {
    // External references
    public static final int TMDB_ID = 1;
    public static final int TVDB_ID = 2;
    public static final int IMDB_ID = 3;
    public static final int IGDB_ID = 4;
    public static final int YT_ID = 5;

    // Media Type references
    public static final int SERIE_ID = 1;
    public static final int GAME_ID = 2;
    public static final int VN_ID = 3;
    public static final int MOVIE_ID = 4;
    public static final int ANIME_SERIE_ID = 5;
    public static final int UNDEFINED_MEDIA_ID = 6;
    public static final int ANIME_MOVIE_ID = 7;

    /**
     * This method update all information from provide <code>MediaModel</code>.
     * <p>
     * @param media this <code>MediaModel</code> object has to have <code>mediaCategory</code> object.
     * @param result this <code>MediaModel</code> object has to have <code>externalReference</code> object.
     * @param typeReference reference source of information to the Media.
     * @param mediaApi implementation of MediaAPI.
     * <p>
     * @return <code>MediaModel</code> object with updated information about the provide Media.
     */
    default public MediaModel updateAllInformation(MediaModel media, @Nullable MediaModel result, TypeReferenceModel typeReference, MediaApi mediaApi) {
        throw new UnsupportedOperationException();
    }

    default public MediaModel updateAllInformation(MediaModel media, @Nullable MediaModel result) {
        throw new UnsupportedOperationException();
    }

    default public MediaModel updateArtworks(MediaModel media, @Nullable MediaModel result, TypeReferenceModel typeReference, MediaApi mediaApi) {
        throw new UnsupportedOperationException();
    }

    default public MediaModel updateArtworks(MediaModel media, @Nullable MediaModel result) {
        throw new UnsupportedOperationException();
    }

    default public List<AlternativeTitleModel> updateAlternativeTitles(MediaModel media, @Nullable MediaModel result, TypeReferenceModel typeReference, MediaApi mediaApi) {
        throw new UnsupportedOperationException();
    }

    default public List<ExternalReferenceModel> updateExternalReferences(MediaModel media, @Nullable MediaModel result, TypeReferenceModel typeReference, MediaApi mediaApi) {
        throw new UnsupportedOperationException();
    }

    default public List<GenreModel> updateGenres(MediaModel media, @Nullable MediaModel result, TypeReferenceModel typeReference, MediaApi mediaApi) {
        throw new UnsupportedOperationException();
    }

    default public List<SeasonModel> updateSeason(MediaModel media, @Nullable MediaModel result, TypeReferenceModel typeReference, MediaApi mediaApi) {
        throw new UnsupportedOperationException();
    }

    default public List<MediaModel> searchMedia(String search, MediaApi mediaApi, TypeReferenceModel typeReference, MediaCategoryModel mediaCategory) {
        throw new UnsupportedOperationException();
    }

    default public MediaModel updateBasicAttributes(MediaModel media, @Nullable MediaModel result, TypeReferenceModel typeReference, MediaApi mediaApi) {
        throw new UnsupportedOperationException();
    }

    default public MediaModel createMediaIfNotExistAndIfExistReturnIt(MediaModel media, TypeReferenceModel typeReference) {
        throw new UnsupportedOperationException();
    }
}
