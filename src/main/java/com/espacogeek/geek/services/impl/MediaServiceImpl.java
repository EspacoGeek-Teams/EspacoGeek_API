package com.espacogeek.geek.services.impl;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Map;

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
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.transaction.Transactional;

import static com.espacogeek.geek.utils.TextUtils.capitalize;

/**
 * A Implementation class of MediaService @see MediaService
 */
@SuppressWarnings({"SpringQualifierCopyableLombok", "OptionalGetWithoutIsPresent", "unchecked"})
@Service
public class MediaServiceImpl implements MediaService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MediaServiceImpl.class);

    @SuppressWarnings("rawtypes")
    private final MediaRepository mediaRepository;

    @SuppressWarnings("rawtypes")
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

    public MediaServiceImpl(
            MediaRepository mediaRepository,
            ExternalReferenceRepository externalsRepo,
            MediaCategoryService mediaCategoryService,
            @Lazy @Qualifier("serieController") MediaDataController serieController,
            @Qualifier("genericMediaDataController") MediaDataController genericMediaDataController,
            TypeReferenceService typeReferenceService,
            @Qualifier("gamesAndVNsAPI") MediaApi gamesAndVNsAPI,
            @Qualifier("movieAPI") MediaApi movieAPI
    ) {
        this.mediaRepository = mediaRepository;
        this.externalsRepo = externalsRepo;
        this.mediaCategoryService = mediaCategoryService;
        this.serieController = serieController;
        this.genericMediaDataController = genericMediaDataController;
        this.typeReferenceService = typeReferenceService;
        this.gamesAndVNsAPI = gamesAndVNsAPI;
        this.movieAPI = movieAPI;
    }

    /**
     * @see MediaService#save(MediaModel)
     */
    @SuppressWarnings("unchecked")
    @Override
    public MediaModel save(MediaModel media) {
        boolean hasInMemoryRefs = media.getExternalReference() != null && !media.getExternalReference().isEmpty();
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
    public List<MediaModel> saveAll(List<MediaModel> medias) {
        return this.mediaRepository.saveAll(medias);
    }

    /**
     * @see MediaService#findSerieByIdOrName(Integer, String, Pageable)
     */
    @SuppressWarnings("unchecked")
    @Override
    public MediaPage findSerieByIdOrName(Integer id, String name, Pageable pageable) {
        Page<MediaModel> results;
        if (id != null) {
            List<MediaModel> medias = new ArrayList<MediaModel>();
            this.mediaRepository.findById(id).ifPresent(media -> medias.add((MediaModel) media));
            Pageable safePageable = pageable != null ? pageable : Pageable.unpaged();
            results = new PageImpl<>(medias, safePageable, medias.size());
        } else {
            results = this.mediaRepository.findMediaByNameOrAlternativeTitleAndMediaCategory(name, name, mediaCategoryService.findById(MediaDataController.MediaType.SERIE.getId()).get().getId(), pageable);
        }
        return mountMediaPage(results);
    }

    /**
     * @see MediaService#findSerieByIdOrName(Integer, String, Map, Pageable)
     */
    @SuppressWarnings("unchecked")
    @Override
    public MediaPage findSerieByIdOrName(Integer id, String name, Map<String, List<String>> requestedFields, Pageable pageable) {
        Page<MediaModel> results;
        if (id != null) {
            List<MediaModel> medias = new ArrayList<MediaModel>();
            this.mediaRepository.findById(id).ifPresent(media -> medias.add((MediaModel) media));
            Pageable safePageable = pageable != null ? pageable : Pageable.unpaged();
            results = new PageImpl<>(medias, safePageable, medias.size());
        } else {
            results = mediaRepository.findMediaByNameOrAlternativeTitleAndMediaCategory(name, name, mediaCategoryService.findById(MediaDataController.MediaType.SERIE.getId()).get().getId(), requestedFields, pageable);
        }
        return mountMediaPage(results);
    }

    /**
     * @see MediaService#findGameByIdOrName(Integer, String, Pageable)
     */
    @SuppressWarnings("unchecked")
    @Override
    public MediaPage findGameByIdOrName(Integer id, String name, Pageable pageable) {
        Page<MediaModel> results;
        if (id != null) {
            List<MediaModel> medias = new ArrayList<MediaModel>();
            this.mediaRepository.findById(id).ifPresent(media -> medias.add((MediaModel) media));
            Pageable safePageable = pageable != null ? pageable : Pageable.unpaged();
            results = new PageImpl<>(medias, safePageable, medias.size());
        } else {
            results = this.mediaRepository.findMediaByNameOrAlternativeTitleAndMediaCategory(name, name, mediaCategoryService.findById(MediaDataController.MediaType.GAME.getId()).get().getId(), pageable);
        }
        return mountMediaPage(results);
    }

    @Override
    @SuppressWarnings("unchecked")
    public MediaPage findVisualNovelByIdOrName(Integer id, String name, Pageable pageable) {
        Page<MediaModel> results;

        if(id != null) {
            List<MediaModel> medias = new ArrayList<MediaModel>();
            this.mediaRepository.findById(id).ifPresent(media -> medias.add((MediaModel) media));
            Pageable safePageable = pageable != null ? pageable : Pageable.unpaged();
            results = new PageImpl<>(medias, safePageable, medias.size());
        } else {
            results = this.mediaRepository.findMediaByNameOrAlternativeTitleAndMediaCategory(name, name, mediaCategoryService.findById(MediaDataController.MediaType.VN.getId()).get().getId(), pageable);
        }
        return mountMediaPage(results);
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
        var fieldList = new ArrayList<Field>();
        MediaModel media = (MediaModel) mediaRepository.findById(id).orElseGet(null);

        if (media == null)
            return Optional.empty();

        for (Field field : media.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(OneToMany.class) || field.isAnnotationPresent(ManyToMany.class)) {
                fieldList.add(field);
            }
        }

        for (Field field : fieldList) {
            try {
                String getterName = "get" + capitalize(field.getName());
                Method getter = media.getClass().getMethod(getterName);
                var fieldValue = getter.invoke(media);
                if (fieldValue instanceof List) {
                    ((List<?>) fieldValue).size(); // This will initialize the collection
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                log.error("Failed to initialize field {} for media id={}: {}", field.getName(), media.getId(), e.getMessage());
            }
        }

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

                MediaModel updated = media;
                updated = switch (categoryId) {
                    case 2, 3 -> MediaUtils
                        .updateGenericMedia(
                            List.of(media),
                            genericMediaDataController,
                            typeReferenceService.findById(MediaDataController.ExternalReferenceType.IGDB.getId()).orElseThrow(),
                            gamesAndVNsAPI)
                        .getFirst();
                    case 1 -> MediaUtils.updateMedia(List.of(media), serieController).getFirst();
                    default -> updated;
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

    /**
     * @see MediaService#findMovieByIdOrName(Integer, String, Pageable)
     */
    @Override
    public MediaPage findMovieByIdOrName(Integer id, String name, Map<String, List<String>> requestedFields, Pageable pageable) {
        Page<MediaModel> results;
        if (id != null) {
            List<MediaModel> medias = new ArrayList<MediaModel>();
            this.mediaRepository.findById(id).ifPresent(media -> medias.add((MediaModel) media));
            results = new PageImpl<>(medias, pageable, medias.size());
        } else {
            results = mediaRepository.findMediaByNameOrAlternativeTitleAndMediaCategory(name, name, mediaCategoryService.findById(MediaDataController.MediaType.MOVIE.getId()).get().getId(), pageable);
        }
        return mountMediaPage(results);
    }

    @Override
    @SuppressWarnings("unchecked")
    public MediaPage findAnimeByIdOrName(Integer id, String name, Pageable pageable) {
        Page<MediaModel> results;
        if (id != null) {
            List<MediaModel> medias = new ArrayList<MediaModel>();
            this.mediaRepository.findById(id).ifPresent(media -> medias.add((MediaModel) media));
            Pageable safePageable = pageable != null ? pageable : Pageable.unpaged();
            results = new PageImpl<>(medias, safePageable, medias.size());
        } else {
            results = (Page<MediaModel>) this.mediaRepository.findMediaByNameOrAlternativeTitleAndMediaCategory(name, name, mediaCategoryService.findById(MediaDataController.MediaType.ANIME_SERIE.getId()).get().getId(), pageable);
        }
        return mountMediaPage(results);
    }

    /**
     * @see MediaService#findMovieByIdOrName(Integer, String, Pageable)
     */
    @SuppressWarnings("unchecked")
    @Override
    public MediaPage findMovieByIdOrName(Integer id, String name, Pageable pageable) {
        Page<MediaModel> results;
        if (id != null) {
            List<MediaModel> medias = new ArrayList<MediaModel>();
            this.mediaRepository.findById(id).ifPresent(media -> medias.add((MediaModel) media));
            Pageable safePageable = pageable != null ? pageable : Pageable.unpaged();
            results = new PageImpl<>(medias, safePageable, medias.size());
        } else {
            results = (Page<MediaModel>) this.mediaRepository.findMediaByNameOrAlternativeTitleAndMediaCategory(name, name, mediaCategoryService.findById(MediaDataController.MediaType.MOVIE.getId()).get().getId(), pageable);
        }
        return mountMediaPage(results);
    }

    private MediaModel update(MediaModel media) {
        return switch (media.getMediaCategory().getId()) {
            case 1 -> MediaUtils.updateMedia(List.of(media), serieController).getFirst();
            case 4, 7, 5 ->
                MediaUtils.updateGenericMedia(List.of(media), genericMediaDataController, typeReferenceService.findById(MediaDataController.ExternalReferenceType.TMDB.getId()).get(), movieAPI).getFirst();
            case 2, 3 ->
                MediaUtils.updateGenericMedia(List.of(media), genericMediaDataController, typeReferenceService.findById(MediaDataController.ExternalReferenceType.IGDB.getId()).get(), gamesAndVNsAPI).getFirst();
            default -> media;
        };

    }

    private MediaPage mountMediaPage(Page<MediaModel> medias) {
        MediaPage response = new MediaPage();

        response.setTotalPages(medias.getTotalPages());
        response.setTotalElements(medias.getTotalElements());
        response.setNumber(medias.getNumber());
        response.setSize(medias.getSize());

        var mediasList = medias.getContent();
        for (MediaModel mediaModel : mediasList) {
            if (MediaUtils.updateMediaWhenLastTimeUpdateMoreThanOneDay(mediaModel)) {
                switch (mediaModel.getMediaCategory().getId()) {
                    case 5:
                    case 1:
                        serieController.updateArtworks(mediaModel, null);
                        break;
                    case 7:
                    case 4:
                        genericMediaDataController.updateArtworks(mediaModel, null, typeReferenceService.findById(MediaDataController.ExternalReferenceType.TMDB.getId()).get(), movieAPI);
                        break;
                    case 2:
                    case 3:
                        genericMediaDataController.updateArtworks(mediaModel, null, typeReferenceService.findById(MediaDataController.ExternalReferenceType.IGDB.getId()).get(), gamesAndVNsAPI);
                        break;
                }
            }
        }

        response.setContent(MediaSimplefied.fromMediaModelList(mediasList));

        return response;
    }
}
