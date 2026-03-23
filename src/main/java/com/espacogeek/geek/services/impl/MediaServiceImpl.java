package com.espacogeek.geek.services.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.espacogeek.geek.data.MediaDataController;
import com.espacogeek.geek.data.api.MediaApi;
import com.espacogeek.geek.models.ExternalReferenceModel;
import com.espacogeek.geek.models.MediaModel;
import com.espacogeek.geek.models.TypeReferenceModel;
import com.espacogeek.geek.repositories.ExternalReferenceRepository;
import com.espacogeek.geek.repositories.MediaRepository;
import com.espacogeek.geek.services.MediaCategoryService;
import com.espacogeek.geek.services.MediaService;
import com.espacogeek.geek.services.TypeReferenceService;
import com.espacogeek.geek.types.MediaPage;
import com.espacogeek.geek.types.MediaSimplefied;
import com.espacogeek.geek.utils.MediaUtils;
import com.espacogeek.geek.utils.MediaLazyLoader;
import jakarta.transaction.Transactional;
import org.hibernate.Hibernate;

/**
 * A Implementation class of MediaService @see MediaService
 */
@SuppressWarnings({"SpringQualifierCopyableLombok", "OptionalGetWithoutIsPresent", "unchecked"})
@Service
public class MediaServiceImpl implements MediaService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MediaServiceImpl.class);

    private final MediaRepository mediaRepository;

    private final ExternalReferenceRepository externalsRepo;

    private final MediaCategoryService mediaCategoryService;

    @Qualifier("serieController")
    private final MediaDataController serieController;

    @Qualifier("genericMediaDataController")
    private final MediaDataController genericMediaDataController;

    private final TypeReferenceService typeReferenceService;

    @Qualifier("gamesAndVNsAPI")
    private final MediaApi gamesAndVNsAPI;

    @Qualifier("movieAPI")
    private final MediaApi movieAPI;

    @Qualifier("tvSeriesApi")
    private final MediaApi tvSeriesApi;

    private final MediaLazyLoader mediaLazyLoader;

    public MediaServiceImpl(
            MediaRepository mediaRepository,
            ExternalReferenceRepository externalsRepo,
            MediaCategoryService mediaCategoryService,
            @Lazy @Qualifier("serieController") MediaDataController serieController,
            @Qualifier("genericMediaDataController") MediaDataController genericMediaDataController,
            TypeReferenceService typeReferenceService,
            @Qualifier("gamesAndVNsAPI") MediaApi gamesAndVNsAPI,
            @Qualifier("movieAPI") MediaApi movieAPI,
            @Qualifier("tvSeriesApi") MediaApi tvSeriesApi,
            MediaLazyLoader mediaLazyLoader
    ) {
        this.mediaRepository = mediaRepository;
        this.externalsRepo = externalsRepo;
        this.mediaCategoryService = mediaCategoryService;
        this.serieController = serieController;
        this.genericMediaDataController = genericMediaDataController;
        this.typeReferenceService = typeReferenceService;
        this.gamesAndVNsAPI = gamesAndVNsAPI;
        this.movieAPI = movieAPI;
        this.tvSeriesApi = tvSeriesApi;
        this.mediaLazyLoader = mediaLazyLoader;
    }

    /**
     * @see MediaService#save(MediaModel)
     */
    @SuppressWarnings("unchecked")
    @Override
    @Transactional
    public MediaModel save(MediaModel media) {
        boolean hasInMemoryRefs = media.getExternalReference() != null
                && Hibernate.isInitialized(media.getExternalReference())
                && !media.getExternalReference().isEmpty();
        boolean hasDbRefs = media.getId() != null && externalsRepo.existsByMediaId(media.getId());
        if (!hasInMemoryRefs && !hasDbRefs) {
            throw new ValidationException("Referência externa obrigatória");
        }
        return (MediaModel) this.mediaRepository.save(media);
    }

    /**
     * @see MediaService#saveAll(List)
     */
    @SuppressWarnings("unchecked")
    @Override
    @Transactional
    public List<MediaModel> saveAll(List<MediaModel> medias) {
        List<MediaModel> validToSave = new ArrayList<>();
        for (MediaModel media : medias) {
            boolean hasInMemoryRefs = media.getExternalReference() != null
                    && Hibernate.isInitialized(media.getExternalReference())
                    && !media.getExternalReference().isEmpty();
            boolean hasDbRefs = media.getId() != null && externalsRepo.existsByMediaId(media.getId());
            if (!hasInMemoryRefs && !hasDbRefs) {
                log.warn("Skipping media '{}' - missing external reference (Referência externa obrigatória)", media.getName());
            } else {
                validToSave.add(media);
            }
        }
        if (validToSave.isEmpty()) {
            return List.of();
        }
        return this.mediaRepository.saveAll(validToSave);
    }

    /**
     * @see MediaService#findSerieByIdOrName(Integer, String, Pageable)
     */
    @SuppressWarnings("unchecked")
    @Override
    public MediaPage findSerieByIdOrName(Integer id, String name, Pageable pageable) {
        return findTmdbMediaByIdOrName(id, name, pageable, MediaDataController.MediaType.SERIE, tvSeriesApi);
    }


    /**
     * @see MediaService#findGameByIdOrName(Integer, String, Pageable)
     */
    @SuppressWarnings("unchecked")
    @Override
    public MediaPage findGameByIdOrName(Integer id, String name, Pageable pageable) {
        return findGenericMediaByIdOrName(id, name, pageable, MediaDataController.MediaType.GAME);
    }

    @Override
    @SuppressWarnings("unchecked")
    public MediaPage findVisualNovelByIdOrName(Integer id, String name, Pageable pageable) {
        return findGenericMediaByIdOrName(id, name, pageable, MediaDataController.MediaType.VN);
    }

    /**
     * @see MediaService#findByReferenceAndTypeReference(ExternalReferenceModel,
     *      TypeReferenceModel)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Optional<MediaModel> findByReferenceAndTypeReference(ExternalReferenceModel reference, TypeReferenceModel typeReference) {
        return this.mediaRepository.findOneMediaByExternalReferenceAndTypeReference(reference.getReference(), typeReference);
    }

    /**
     * @see MediaService#findByIdEager(Integer)
     */
    @SuppressWarnings({"unchecked", "ResultOfMethodCallIgnored"})
    @Override
    @Transactional
    public Optional<MediaModel> findByIdEager(Integer id) {
        MediaModel media = (MediaModel) mediaRepository.findById(id).orElse(null);

        if (media == null)
            return Optional.empty();

        mediaLazyLoader.initializeCollections(media);

        media = update(media);

        return Optional.ofNullable(media);
    }

    /**
     * @see MediaService#randomArtwork()
     */
    @Override
    @Transactional
    public Optional<String> randomArtwork() {
        long total = mediaRepository.count();
        if (total <= 0)
            return Optional.empty();

        int maxAttempts = (int) Math.min(total + 10, 200); // safety cap
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            int randomIndex = ThreadLocalRandom.current().nextInt((int) total);
            var page = mediaRepository.findAll(PageRequest.of(randomIndex, 1));
            if (page.isEmpty())
                continue;

            MediaModel media = (MediaModel) page.getContent().getFirst();
            if (media == null)
                continue;

            if (media.getBanner() != null && !media.getBanner().isEmpty()) {
                return Optional.of(media.getBanner());
            }

            try {
                // Try to fetch/update artworks for this media
                Integer categoryId = media.getMediaCategory() != null ? media.getMediaCategory().getId() : null;
                if (categoryId == null)
                    continue;

                MediaModel updated = switch (categoryId) {
                    case 2, 3 -> MediaUtils
                        .updateGenericMedia(
                            List.of(media),
                            genericMediaDataController,
                            typeReferenceService.findById(MediaDataController.ExternalReferenceType.IGDB.getId()).orElseThrow(),
                            gamesAndVNsAPI)
                        .getFirst();
                    case 1 -> MediaUtils.updateMedia(List.of(media), serieController).getFirst();
                    default -> media;
                };

                if (updated != null && updated.getBanner() != null && !updated.getBanner().isEmpty()) {
                    return Optional.of(updated.getBanner());
                }
            } catch (Exception ex) {
                log.warn("Failed to update artwork for media id={}: {}", media.getId(), ex.getMessage());
                // continue to next attempt
            }
        }

        return Optional.empty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public MediaPage findAnimeByIdOrName(Integer id, String name, Pageable pageable) {
        Pageable safePageable = pageable != null ? pageable : Pageable.unpaged();

        if (id != null) {
            List<MediaModel> medias = new ArrayList<>();
            this.mediaRepository.findById(id).ifPresent(media -> medias.add((MediaModel) media));
            return mountMediaPage(new PageImpl<>(medias, safePageable, medias.size()));
        }

        var animeSerie = mediaCategoryService.findById(MediaDataController.MediaType.ANIME_SERIE.getId()).orElseThrow();
        var animeMovie = mediaCategoryService.findById(MediaDataController.MediaType.ANIME_MOVIE.getId()).orElseThrow();
        List<Integer> animeCategories = List.of(animeSerie.getId(), animeMovie.getId());

        Page<MediaModel> results = (Page<MediaModel>) this.mediaRepository
                .findMediaByNameOrAlternativeTitleAndMediaCategoryIn(name, name, animeCategories, safePageable);

        if (results.hasContent()) {
            List<MediaModel> refreshed = results.getContent().stream()
                    .map(this::updateIfStale)
                    .toList();
            return mountMediaPage(new PageImpl<>(refreshed, safePageable, results.getTotalElements()));
        }

        if (name == null || name.isBlank()) {
            return mountMediaPage(Page.empty(safePageable));
        }

        var tmdbTypeReference = typeReferenceService.findById(MediaDataController.ExternalReferenceType.TMDB.getId()).orElseThrow();
        List<MediaModel> seriesFetched = genericMediaDataController.searchMedia(name, tvSeriesApi, tmdbTypeReference, animeSerie);
        List<MediaModel> movieFetched = genericMediaDataController.searchMedia(name, movieAPI, tmdbTypeReference, animeMovie);

        List<MediaModel> fetched = new ArrayList<>();
        fetched.addAll(seriesFetched);
        fetched.addAll(movieFetched);

        if (fetched.isEmpty()) {
            return mountMediaPage(Page.empty(safePageable));
        }

        int fromIndex = Math.min((int) safePageable.getOffset(), fetched.size());
        int toIndex = safePageable.isPaged()
                ? Math.min(fromIndex + safePageable.getPageSize(), fetched.size())
                : fetched.size();
        List<MediaModel> pagedContent = fetched.subList(fromIndex, toIndex);

        return mountMediaPage(new PageImpl<>(pagedContent, safePageable, fetched.size()));
    }

    /**
     * @see MediaService#findMovieByIdOrName(Integer, String, Pageable)
     */
    @SuppressWarnings("unchecked")
    @Override
    public MediaPage findMovieByIdOrName(Integer id, String name, Pageable pageable) {
        return findTmdbMediaByIdOrName(id, name, pageable, MediaDataController.MediaType.MOVIE, movieAPI);
    }

    private MediaModel update(MediaModel media) {
        return switch (media.getMediaCategory().getId()) {
            case 1 -> MediaUtils.updateMedia(List.of(media), serieController).getFirst();
            case 4, 7 ->
                MediaUtils.updateGenericMedia(List.of(media), genericMediaDataController, typeReferenceService.findById(MediaDataController.ExternalReferenceType.TMDB.getId()).get(), movieAPI).getFirst();
            case 5 ->
                MediaUtils.updateGenericMedia(List.of(media), genericMediaDataController, typeReferenceService.findById(MediaDataController.ExternalReferenceType.TMDB.getId()).get(), tvSeriesApi).getFirst();
            case 2, 3 ->
                MediaUtils.updateGenericMedia(List.of(media), genericMediaDataController, typeReferenceService.findById(MediaDataController.ExternalReferenceType.IGDB.getId()).get(), gamesAndVNsAPI).getFirst();
            default -> media;
        };

    }

    @SuppressWarnings("unchecked")
    private MediaPage findTmdbMediaByIdOrName(Integer id, String name, Pageable pageable, MediaDataController.MediaType mediaType, MediaApi mediaApi) {
        Pageable safePageable = pageable != null ? pageable : Pageable.unpaged();
        var mediaCategory = mediaCategoryService.findById(mediaType.getId()).orElseThrow();
        Page<MediaModel> results;

        if (id != null) {
            List<MediaModel> medias = new ArrayList<>();
            this.mediaRepository.findById(id).ifPresent(media -> medias.add((MediaModel) media));
            results = new PageImpl<>(medias, safePageable, medias.size());
            return mountMediaPage(results);
        }

        results = (Page<MediaModel>) this.mediaRepository.findMediaByNameOrAlternativeTitleAndMediaCategory(name, name, mediaCategory.getId(), safePageable);

        if (results.hasContent()) {
            List<MediaModel> refreshed = results.getContent().stream()
                    .map(this::updateIfStale)
                    .toList();
            return mountMediaPage(new PageImpl<>(refreshed, safePageable, results.getTotalElements()));
        }

        if (name == null || name.isBlank()) {
            return mountMediaPage(Page.empty(safePageable));
        }

        var tmdbTypeReference = typeReferenceService.findById(MediaDataController.ExternalReferenceType.TMDB.getId()).orElseThrow();
        List<MediaModel> fetched = genericMediaDataController.searchMedia(name, mediaApi, tmdbTypeReference, mediaCategory);

        if (fetched.isEmpty()) {
            return mountMediaPage(Page.empty(safePageable));
        }

        int fromIndex = Math.min((int) safePageable.getOffset(), fetched.size());
        int toIndex = safePageable.isPaged()
                ? Math.min(fromIndex + safePageable.getPageSize(), fetched.size())
                : fetched.size();
        List<MediaModel> pagedContent = fetched.subList(fromIndex, toIndex);

        return mountMediaPage(new PageImpl<>(pagedContent, safePageable, fetched.size()));
    }

    private MediaPage findGenericMediaByIdOrName(Integer id, String name, Pageable pageable, MediaDataController.MediaType mediaType) {
        Pageable safePageable = pageable != null ? pageable : Pageable.unpaged();
        var mediaCategory = mediaCategoryService.findById(mediaType.getId()).orElseThrow();
        Page<MediaModel> results;

        if (id != null) {
            List<MediaModel> medias = new ArrayList<>();
            this.mediaRepository.findById(id)
                    .map(media -> updateIfStale((MediaModel) media))
                    .ifPresent(medias::add);
            results = new PageImpl<>(medias, safePageable, medias.size());
            return mountMediaPage(results);
        }

        results = this.mediaRepository.findMediaByNameOrAlternativeTitleAndMediaCategory(name, name, mediaCategory.getId(), safePageable);

        if (results.hasContent()) {
            List<MediaModel> refreshed = results.getContent().stream()
                    .map(this::updateIfStale)
                    .toList();
            return mountMediaPage(new PageImpl<>(refreshed, safePageable, results.getTotalElements()));
        }

        if (name == null || name.isBlank()) {
            return mountMediaPage(Page.empty(safePageable));
        }

        var igdbTypeReference = typeReferenceService.findById(MediaDataController.ExternalReferenceType.IGDB.getId()).orElseThrow();
        List<MediaModel> fetched = genericMediaDataController.searchMedia(name, gamesAndVNsAPI, igdbTypeReference, mediaCategory);

        if (fetched.isEmpty()) {
            return mountMediaPage(Page.empty(safePageable));
        }

        int fromIndex = Math.min((int) safePageable.getOffset(), fetched.size());
        int toIndex = safePageable.isPaged()
                ? Math.min(fromIndex + safePageable.getPageSize(), fetched.size())
                : fetched.size();
        List<MediaModel> pagedContent = fetched.subList(fromIndex, toIndex);

        return mountMediaPage(new PageImpl<>(pagedContent, safePageable, fetched.size()));
    }

    private MediaModel updateIfStale(MediaModel media) {
        if (media == null) {
            return null;
        }

        return MediaUtils.updateMediaWhenLastTimeUpdateMoreThanOneDay(media)
                ? update(media)
                : media;
    }

    private MediaPage mountMediaPage(Page<MediaModel> medias) {
        MediaPage response = new MediaPage();

        response.setTotalPages(medias.getTotalPages());
        response.setTotalElements(medias.getTotalElements());
        response.setNumber(medias.getNumber());
        response.setSize(medias.getSize());

        response.setContent(MediaSimplefied.fromMediaModelList(medias.getContent()));

        return response;
    }
}
