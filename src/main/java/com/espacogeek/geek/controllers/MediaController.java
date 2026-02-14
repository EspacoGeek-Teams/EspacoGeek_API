package com.espacogeek.geek.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.espacogeek.geek.data.MediaDataController;
import com.espacogeek.geek.data.api.MediaApi;
import com.espacogeek.geek.models.MediaModel;
import com.espacogeek.geek.services.MediaCategoryService;
import com.espacogeek.geek.services.MediaService;
import com.espacogeek.geek.services.TypeReferenceService;
import com.espacogeek.geek.types.MediaPage;
import com.espacogeek.geek.types.MediaSimplefied;
import com.espacogeek.geek.utils.MediaUtils;
import com.espacogeek.geek.exception.GenericException;

import graphql.schema.DataFetchingEnvironment;

@Controller
public class MediaController {
    @Autowired
    private MediaService mediaService;

    /**
     * Finds a MediaModel object by its ID.
     *
     * @param id The ID of the MediaModel object to find.
     * @return The MediaModel object that matches the provided ID.
     * @throws GenericException if the MediaModel object is not found.
     */
    @QueryMapping(name = "media")
    public MediaModel getMediaById(@Argument(name = "id") Integer id) {
        var media = this.mediaService.findByIdEager(id).orElseThrow(() -> new GenericException("Media not found"));
        return media;
    }

    /**
     * Finds Movie (MediaModel) objects by their ID or name.
     *
     * @param id   The ID of the Movie (MediaModel) object to find.
     * @param name The name of the Movie (MediaModel) object to find.
     * @return A list of Movie (MediaModel) objects that match the provided ID or
     *         name.
     */
    @QueryMapping(name = "movie")
    public MediaPage getMovie(@Argument(name = "id") Integer id, @Argument(name = "name") String name, DataFetchingEnvironment dataFetchingEnvironment) {
        name = name == null ? null : name.trim();

        if (name == null & id == null || name == "" & id == null) {
            return new MediaPage();
        }

        return this.mediaService.findMovieByIdOrName(id, name, MediaUtils.getPageable(dataFetchingEnvironment));
    }

    /**
     * Finds Series (MediaModel) objects by their ID or name.
     *
     * @param id   The ID of the Series (MediaModel) object to find.
     * @param name The name of the Series (MediaModel) object to find.
     * @return A list of Series (MediaModel) objects that match the provided ID or
     *         name.
     */
    @QueryMapping(name = "tvserie")
    public MediaPage getSerie(@Argument(name = "id") Integer id, @Argument(name = "name") String name, DataFetchingEnvironment dataFetchingEnvironment) {
        name = name == null ? null : name.trim();

        if (name == null & id == null || name == "" & id == null) {
            return new MediaPage();
        }

        return this.mediaService.findSerieByIdOrName(id, name, MediaUtils.getPageable(dataFetchingEnvironment));
    }

    /**
     * Finds Game (MediaModel) objects by their ID or name. When searching by ID, the name parameter is not used amd all fields are updated.
     *
     * @param id   The ID of the Game (MediaModel) object to find.
     * @param name The name of the Game (MediaModel) object to find.
     * @return A list of Game (MediaModel) objects that match the provided ID or
     *         name.
     */
    @QueryMapping(name = "game")
    public MediaPage getGame(@Argument(name = "id") Integer id, @Argument(name = "name") String name, DataFetchingEnvironment dataFetchingEnvironment) {
        name = name == null ? null : name.trim();

        if (name == null & id == null || name == "" & id == null) {
            return new MediaPage();
        }

        return this.mediaService.findGameByIdOrName(id, name, MediaUtils.getPageable(dataFetchingEnvironment));
    }

    /**
     * Finds Visual Novel (MediaModel) objects by their ID or name.
     *
     * @param id   The ID of the Visual Novel (MediaModel) object to find.
     * @param name The name of the Visual Novel (MediaModel) object to find.
     * @return A list of Visual Novel (MediaModel) objects that match the provided
     *         ID or name.
     */
    @QueryMapping(name = "vn")
    public MediaPage getVisualNovel(@Argument(name = "id") Integer id, @Argument(name = "name") String name, DataFetchingEnvironment dataFetchingEnvironment) {
        MediaPage response = new MediaPage();
        name = name == null ? null : name.trim();

        if (name == null & id == null || name == "" & id == null) {
            return response;
        }

        return this.mediaService.findVisualNovelByIdOrName(id, name, MediaUtils.getPageable(dataFetchingEnvironment));
    }

    /**
     * Finds Anime (MediaModel) objects by their ID or name.
     *
     * @param id   The ID of the Anime (MediaModel) object to find.
     * @param name The name of the Anime (MediaModel) object to find.
     * @return A list of Anime (MediaModel) objects that match the provided ID or name.
     */
    @QueryMapping(name = "anime")
    public MediaPage getAnime(@Argument(name = "id") Integer id, @Argument(name = "name") String name, DataFetchingEnvironment dataFetchingEnvironment) {
        name = name == null ? null : name.trim();

        if (name == null & id == null || name == "" & id == null) {
            return new MediaPage();
        }

        return this.mediaService.findAnimeByIdOrName(id, name, MediaUtils.getPageable(dataFetchingEnvironment));
    }
}
