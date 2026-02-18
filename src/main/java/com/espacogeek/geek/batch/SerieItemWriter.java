package com.espacogeek.geek.batch;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;

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
public class SerieItemWriter implements ItemWriter<MediaModel> {
    private final MediaService mediaService;
    private final ExternalReferenceService externalReferenceService;
    private final AlternativeTitlesService alternativeTitlesService;
    @Qualifier("tvSeriesApi")
    private final MediaApi tvSeriesApi;
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

        List<MediaModel> toSave = new ArrayList<>(items);
        List<MediaModel> saved;
        try {
            saved = mediaService.saveAll(toSave);
        } catch (Exception e) {
            log.error("Failed to save series batch: {}", e.getMessage(), e);
            throw e;
        }

        List<ExternalReferenceModel> refsToSave = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            MediaModel original = items.get(i);
            MediaModel persisted = saved.size() > i ? saved.get(i) : null;
            if (persisted == null) continue;

            if (original.getExternalReference() != null) {
                for (ExternalReferenceModel ref : original.getExternalReference()) {
                    ref.setMedia(persisted);
                    refsToSave.add(ref);
                }
            }

            try {
                String externalId = null;
                if (original.getExternalReference() != null && !original.getExternalReference().isEmpty()) {
                    externalId = original.getExternalReference().get(0).getReference();
                }
                if (externalId != null) {
                    var alts = tvSeriesApi.getAlternativeTitles(Integer.valueOf(externalId));
                    if (alts != null && !alts.isEmpty()) {
                        alternativeTitlesService.saveAll(alts);
                        persisted.setAlternativeTitles(alts);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to fetch/save alternative titles for series: {}", e.getMessage());
            }
        }

        if (!refsToSave.isEmpty()) {
            try {
                externalReferenceService.saveAll(refsToSave);
            } catch (Exception e) {
                log.error("Failed to save external references for series: {}", e.getMessage(), e);
            }
        }
    }

    @Override
    public void write(org.springframework.batch.item.Chunk<? extends MediaModel> chunk) throws Exception {
        if (chunk == null) return;
        write(chunk.getItems());
    }
}
