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
import lombok.Getter;

public interface MediaDataController {

    /**
     * This enum is used to define the type of external reference.
     */
    @Getter
    enum ExternalReferenceType {
        TMDB(1),
        TVDB(2),
        IMDB(3),
        IGDB(4),
        YT(5);

        private final int id;

        ExternalReferenceType(int id) {
            this.id = id;
        }

    }

    /**
     * This enum is used to define the type of media.
     * see randomArtwork
     */
    @Getter
    enum MediaType {
        SERIE(1),
        GAME(2),
        VN(3),
        MOVIE(4),
        ANIME_SERIE(5),
        UNDEFINED_MEDIA(6),
        ANIME_MOVIE(7);

        private final int id;

        MediaType(int id) {
            this.id = id;
        }
    }

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
    default MediaModel updateAllInformation(MediaModel media, @Nullable MediaModel result, TypeReferenceModel typeReference, MediaApi mediaApi) {
        throw new UnsupportedOperationException();
    }

    default MediaModel updateAllInformation(MediaModel media, @Nullable MediaModel result) {
        throw new UnsupportedOperationException();
    }

    default MediaModel updateArtworks(MediaModel media, @Nullable MediaModel result, TypeReferenceModel typeReference, MediaApi mediaApi) {
        throw new UnsupportedOperationException();
    }

    default MediaModel updateArtworks(MediaModel media, @Nullable MediaModel result) {
        throw new UnsupportedOperationException();
    }

    default List<AlternativeTitleModel> updateAlternativeTitles(MediaModel media, @Nullable MediaModel result, TypeReferenceModel typeReference, MediaApi mediaApi) {
        throw new UnsupportedOperationException();
    }

    default List<ExternalReferenceModel> updateExternalReferences(MediaModel media, @Nullable MediaModel result, TypeReferenceModel typeReference, MediaApi mediaApi) {
        throw new UnsupportedOperationException();
    }

    default List<GenreModel> updateGenres(MediaModel media, @Nullable MediaModel result, TypeReferenceModel typeReference, MediaApi mediaApi) {
        throw new UnsupportedOperationException();
    }

    default List<SeasonModel> updateSeason(MediaModel media, @Nullable MediaModel result, TypeReferenceModel typeReference, MediaApi mediaApi) {
        throw new UnsupportedOperationException();
    }

    default List<MediaModel> searchMedia(String search, MediaApi mediaApi, TypeReferenceModel typeReference, MediaCategoryModel mediaCategory) {
        throw new UnsupportedOperationException();
    }

    default MediaModel updateBasicAttributes(MediaModel media, @Nullable MediaModel result, TypeReferenceModel typeReference, MediaApi mediaApi) {
        throw new UnsupportedOperationException();
    }

    default MediaModel createMediaIfNotExistAndIfExistReturnIt(MediaModel media, TypeReferenceModel typeReference) {
        throw new UnsupportedOperationException();
    }
}
