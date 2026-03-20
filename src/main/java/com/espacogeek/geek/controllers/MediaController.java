package com.espacogeek.geek.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.espacogeek.geek.models.AlternativeTitleModel;
import com.espacogeek.geek.models.CompanyModel;
import com.espacogeek.geek.models.ExternalReferenceModel;
import com.espacogeek.geek.models.GenreModel;
import com.espacogeek.geek.models.MediaModel;
import com.espacogeek.geek.models.PeopleModel;
import com.espacogeek.geek.models.SeasonModel;
import com.espacogeek.geek.repositories.AlternativeTitlesRepository;
import com.espacogeek.geek.repositories.ExternalReferenceRepository;
import com.espacogeek.geek.repositories.MediaRepository;
import com.espacogeek.geek.repositories.SeasonRepository;
import com.espacogeek.geek.services.MediaService;
import com.espacogeek.geek.types.MediaPage;
import com.espacogeek.geek.utils.MediaUtils;
import com.espacogeek.geek.exception.GenericException;

import graphql.schema.DataFetchingEnvironment;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
@Controller
@RequiredArgsConstructor
public class MediaController {
    private final MediaService mediaService;
    private final SeasonRepository seasonRepository;
    private final AlternativeTitlesRepository alternativeTitlesRepository;
    @SuppressWarnings("rawtypes")
    private final ExternalReferenceRepository externalReferenceRepository;    private final MediaRepository mediaRepository;

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

        if (name == null && id == null || name == "" && id == null) {
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

        if (name == null && id == null || name == "" && id == null) {
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

        if (name == null && id == null || name == "" && id == null) {
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

        if (name == null && id == null || name == "" && id == null) {
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

        if (name == null && id == null || name == "" && id == null) {
            return new MediaPage();
        }

        return this.mediaService.findAnimeByIdOrName(id, name, MediaUtils.getPageable(dataFetchingEnvironment));
    }

    /**
     * Batch-loads seasons for a list of MediaModel sources, resolving the N+1 problem.
     */
    @BatchMapping
    public Map<MediaModel, Set<SeasonModel>> season(List<MediaModel> medias) {
        Map<Integer, MediaModel> sourceById = medias.stream()
                .collect(Collectors.toMap(MediaModel::getId, m -> m));
        Map<MediaModel, Set<SeasonModel>> result = medias.stream()
                .collect(Collectors.toMap(m -> m, m -> new HashSet<>()));
        for (SeasonModel season : seasonRepository.findByMediaIn(medias)) {
            MediaModel source = sourceById.get(season.getMedia().getId());
            if (source != null) {
                result.get(source).add(season);
            }
        }
        return result;
    }

    /**
     * Batch-loads genres for a list of MediaModel sources, resolving the N+1 problem.
     */
    @BatchMapping
    public Map<MediaModel, Set<GenreModel>> genre(List<MediaModel> medias) {
        Map<Integer, MediaModel> sourceById = medias.stream()
                .collect(Collectors.toMap(MediaModel::getId, m -> m));
        Map<MediaModel, Set<GenreModel>> result = medias.stream()
                .collect(Collectors.toMap(m -> m, m -> new HashSet<>()));
        for (MediaModel loaded : mediaRepository.findAllWithGenreByMediaIn(medias)) {
            MediaModel source = sourceById.get(loaded.getId());
            if (source != null && loaded.getGenre() != null) {
                result.put(source, loaded.getGenre());
            }
        }
        return result;
    }

    /**
     * Batch-loads companies for a list of MediaModel sources, resolving the N+1 problem.
     */
    @BatchMapping
    public Map<MediaModel, Set<CompanyModel>> company(List<MediaModel> medias) {
        Map<Integer, MediaModel> sourceById = medias.stream()
                .collect(Collectors.toMap(MediaModel::getId, m -> m));
        Map<MediaModel, Set<CompanyModel>> result = medias.stream()
                .collect(Collectors.toMap(m -> m, m -> new HashSet<>()));
        for (MediaModel loaded : mediaRepository.findAllWithCompanyByMediaIn(medias)) {
            MediaModel source = sourceById.get(loaded.getId());
            if (source != null && loaded.getCompany() != null) {
                result.put(source, loaded.getCompany());
            }
        }
        return result;
    }

    /**
     * Batch-loads people for a list of MediaModel sources, resolving the N+1 problem.
     */
    @BatchMapping
    public Map<MediaModel, Set<PeopleModel>> people(List<MediaModel> medias) {
        Map<Integer, MediaModel> sourceById = medias.stream()
                .collect(Collectors.toMap(MediaModel::getId, m -> m));
        Map<MediaModel, Set<PeopleModel>> result = medias.stream()
                .collect(Collectors.toMap(m -> m, m -> new HashSet<>()));
        for (MediaModel loaded : mediaRepository.findAllWithPeopleByMediaIn(medias)) {
            MediaModel source = sourceById.get(loaded.getId());
            if (source != null && loaded.getPeople() != null) {
                result.put(source, loaded.getPeople());
            }
        }
        return result;
    }

    /**
     * Batch-loads external references for a list of MediaModel sources, resolving the N+1 problem.
     */
    @BatchMapping
    @SuppressWarnings("unchecked")
    public Map<MediaModel, Set<ExternalReferenceModel>> externalReference(List<MediaModel> medias) {
        Map<Integer, MediaModel> sourceById = medias.stream()
                .collect(Collectors.toMap(MediaModel::getId, m -> m));
        Map<MediaModel, Set<ExternalReferenceModel>> result = medias.stream()
                .collect(Collectors.toMap(m -> m, m -> new HashSet<>()));
        List<ExternalReferenceModel> refs = externalReferenceRepository.findAllByMediaIn(medias);
        for (ExternalReferenceModel ref : refs) {
            MediaModel source = sourceById.get(ref.getMedia().getId());
            if (source != null) {
                result.get(source).add(ref);
            }
        }
        return result;
    }

    /**
     * Batch-loads alternative titles for a list of MediaModel sources, resolving the N+1 problem.
     */
    @BatchMapping
    public Map<MediaModel, Set<AlternativeTitleModel>> alternativeTitles(List<MediaModel> medias) {
        Map<Integer, MediaModel> sourceById = medias.stream()
                .collect(Collectors.toMap(MediaModel::getId, m -> m));
        Map<MediaModel, Set<AlternativeTitleModel>> result = medias.stream()
                .collect(Collectors.toMap(m -> m, m -> new HashSet<>()));
        for (AlternativeTitleModel alt : alternativeTitlesRepository.findByMediaIn(medias)) {
            MediaModel source = sourceById.get(alt.getMedia().getId());
            if (source != null) {
                result.get(source).add(alt);
            }
        }
        return result;
    }
}
