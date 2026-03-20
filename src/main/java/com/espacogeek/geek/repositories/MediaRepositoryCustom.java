package com.espacogeek.geek.repositories;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import com.espacogeek.geek.models.MediaModel;

public interface MediaRepositoryCustom {

    /**
     * Finds media by matching name or alternative title within a specific media
     * category.
     *
     * This query searches for MediaModel entities where the name or any alternative
     * title matches the provided name or alternativeTitle parameters. It filters
     * the
     * results to only include those within the specified media category. The
     * requestedFields parameter is optional and can be used to return only the
     * selected fields of the entities.
     *
     * @param name             The name of the media to search for.
     * @param alternativeTitle The alternative title of the media to search for.
     * @param category         The ID of the media category to filter results by.
     * @param requestedFields  A map of fields to return. If not provided, all
     *                         fields will be returned.
     * @return a Page of MediaModel objects that match the search criteria.
     */
    public Page<MediaModel> findMediaByNameOrAlternativeTitleAndMediaCategory(
            String name,
            String alternativeTitle,
            Integer category,
            Map<String, List<String>> requestedFields,
            @PageableDefault(size = 10, page = 0) Pageable pageable);
}
