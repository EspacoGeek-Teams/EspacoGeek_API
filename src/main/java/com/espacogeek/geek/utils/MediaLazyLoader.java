package com.espacogeek.geek.utils;

import java.util.LinkedHashSet;

import org.hibernate.Hibernate;
import org.springframework.stereotype.Component;

import com.espacogeek.geek.models.MediaModel;
import com.espacogeek.geek.services.AlternativeTitlesService;
import com.espacogeek.geek.services.ExternalReferenceService;
import com.espacogeek.geek.services.GenreService;
import com.espacogeek.geek.services.SeasonService;

/**
 * Utility Spring bean that guarantees all lazy-loaded {@code Set<>} collections
 * of a {@link MediaModel} are available for use outside an active Hibernate session.
 *
 * <p>With Java 21 virtual threads, thread-local Hibernate sessions can be closed
 * before update methods finish processing a detached entity, causing
 * {@code LazyInitializationException}. This class solves the problem once and for all
 * by checking {@link Hibernate#isInitialized} for every lazy collection and falling
 * back to a fresh repository query when the session is no longer active.
 *
 * <p>Covered collections: {@code externalReference}, {@code alternativeTitles},
 * {@code genre}, {@code season}.
 */
@Component
public class MediaLazyLoader {

    private final ExternalReferenceService externalReferenceService;
    private final AlternativeTitlesService alternativeTitlesService;
    private final GenreService genreService;
    private final SeasonService seasonService;

    public MediaLazyLoader(
            ExternalReferenceService externalReferenceService,
            AlternativeTitlesService alternativeTitlesService,
            GenreService genreService,
            SeasonService seasonService) {
        this.externalReferenceService = externalReferenceService;
        this.alternativeTitlesService = alternativeTitlesService;
        this.genreService = genreService;
        this.seasonService = seasonService;
    }

    /**
     * Ensures all lazy {@link java.util.Set} collections of the given {@link MediaModel}
     * are initialized. For each collection that is {@code null} or not yet initialized
     * by Hibernate (e.g., because the session that loaded the entity has been closed),
     * the data is loaded directly from the corresponding service and set back on the entity.
     *
     * @param media the {@link MediaModel} to initialize; no-op when {@code null} or unsaved
     */
    public void initializeCollections(MediaModel media) {
        if (media == null || media.getId() == null) {
            return;
        }

        if (media.getExternalReference() == null || !Hibernate.isInitialized(media.getExternalReference())) {
            media.setExternalReference(new LinkedHashSet<>(externalReferenceService.findAll(media)));
        }

        if (media.getAlternativeTitles() == null || !Hibernate.isInitialized(media.getAlternativeTitles())) {
            media.setAlternativeTitles(new LinkedHashSet<>(alternativeTitlesService.findAll(media)));
        }

        if (media.getGenre() == null || !Hibernate.isInitialized(media.getGenre())) {
            media.setGenre(new LinkedHashSet<>(genreService.findAll(media)));
        }

        if (media.getSeason() == null || !Hibernate.isInitialized(media.getSeason())) {
            media.setSeason(new LinkedHashSet<>(seasonService.findAll(media)));
        }
    }
}
