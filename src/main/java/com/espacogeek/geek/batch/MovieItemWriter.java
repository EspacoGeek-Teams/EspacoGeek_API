package com.espacogeek.geek.batch;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.batch.item.ItemWriter;

import com.espacogeek.geek.data.api.MediaApi;
import com.espacogeek.geek.models.ExternalReferenceModel;
import com.espacogeek.geek.models.TypeReferenceModel;
import com.espacogeek.geek.models.MediaModel;
import com.espacogeek.geek.services.ExternalReferenceService;
import com.espacogeek.geek.services.MediaService;
import com.espacogeek.geek.services.AlternativeTitlesService;
import com.espacogeek.geek.services.TypeReferenceService;
import com.espacogeek.geek.data.MediaDataController.ExternalReferenceType;

@Component
@RequiredArgsConstructor
@Slf4j
public class MovieItemWriter implements ItemWriter<MediaModel> {
    private final MediaService mediaService;
    private final ExternalReferenceService externalReferenceService;
    private final AlternativeTitlesService alternativeTitlesService;
    @Qualifier("movieAPI")
    private final MediaApi movieApi;
    private final TypeReferenceService typeReferenceService;

    private TypeReferenceModel typeReference;

    @PostConstruct
    public void init() {
        try {
            this.typeReference = typeReferenceService.findById(ExternalReferenceType.TMDB.getId())
                    .orElse(null);
        } catch (Exception e) {
            this.typeReference = null;
        }
    }

    public void write(List<? extends MediaModel> items) {
        if (items == null || items.isEmpty()) return;

        for (MediaModel original : items) {
            MediaModel persisted;
            try {
                persisted = mediaService.save(original);
            } catch (ValidationException e) {
                log.warn("Skipping media '{}' - missing external reference: {}", original.getName(), e.getMessage());
                continue;
            } catch (Exception e) {
                log.error("Failed to save media '{}': {}", original.getName(), e.getMessage(), e);
                continue;
            }

            // Collect external references to save (associate to persisted media)
            List<ExternalReferenceModel> refsToSave = new ArrayList<>();
            if (original.getExternalReference() != null) {
                for (ExternalReferenceModel ref : original.getExternalReference()) {
                    ref.setMedia(persisted);
                    refsToSave.add(ref);
                }
            }

            // fetch and persist alternative titles using the external API id (if present)
            try {
                String externalId = null;
                if (original.getExternalReference() != null && !original.getExternalReference().isEmpty()) {
                    externalId = original.getExternalReference().get(0).getReference();
                }
                if (externalId != null) {
                    var alts = movieApi.getAlternativeTitles(Integer.valueOf(externalId));
                    if (alts != null && !alts.isEmpty()) {
                        alternativeTitlesService.saveAll(alts);
                        persisted.setAlternativeTitles(alts);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to fetch/save alternative titles: {}", e.getMessage());
            }

            if (!refsToSave.isEmpty()) {
                try {
                    externalReferenceService.saveAll(refsToSave);
                } catch (Exception e) {
                    log.error("Failed to save external references: {}", e.getMessage(), e);
                }
            }
        }
    }

    // Spring Batch 5+ uses Chunk-based ItemWriter signature. Delegate to the list-based implementation if needed.
    @Override
    public void write(org.springframework.batch.item.Chunk<? extends MediaModel> chunk) throws Exception {
        if (chunk == null) return;
        write(chunk.getItems());
    }
}
