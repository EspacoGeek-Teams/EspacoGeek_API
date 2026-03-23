package com.espacogeek.geek.repositories;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Repository;

import com.espacogeek.geek.models.MediaModel;
import com.espacogeek.geek.models.TypeReferenceModel;

@Repository
public interface MediaRepository extends JpaRepository<MediaModel, Integer> {

    // if at some time the queries become more complex, see https://www.jooq.org/
    // and https://persistence.blazebit.com/.

    /**
     * Finds media by matching name or alternative title within a specific media
     * category.
     *
     * This query searches for MediaModel entities where the name or any alternative
     * title matches the provided name or alternativeTitle parameters. It filters
     * the
     * results to only include those within the specified media category.
     *
     * @param name             The name of the media to search for.
     * @param alternativeTitle The alternative title of the media to search for.
     * @param category         The ID of the media category to filter results by.
     * @param pageable         Pagination information.
     * @return A list of MediaModel objects that match the search criteria.
     */
@Query("SELECT m FROM MediaModel m " +
           "WHERE m.mediaCategory.id = :category " +
           "AND (" +
           "   m.name LIKE CONCAT('%', :name, '%') " +
           "   OR EXISTS (" +
           "       SELECT 1 FROM AlternativeTitleModel a " +
           "       WHERE a.media = m " +
           "       AND a.name LIKE CONCAT('%', :alternativeTitle, '%')" +
           "   )" +
           ")")
    Page<MediaModel> findMediaByNameOrAlternativeTitleAndMediaCategory(
            @Param("name") String name,
            @Param("alternativeTitle") String alternativeTitle,
            @Param("category") Integer category,
            @PageableDefault(size = 10, page = 0) Pageable pageable);

    /**
     * Finds media by matching name or alternative title within multiple media
     * categories.
     *
     * @param name             The name of the media to search for.
     * @param alternativeTitle The alternative title of the media to search for.
     * @param categories       The IDs of the media categories to filter results by.
     * @param pageable         Pagination information.
     * @return A page of MediaModel objects that match the search criteria.
     */
    @Query("SELECT m FROM MediaModel m " +
               "WHERE m.mediaCategory.id IN :categories " +
               "AND (" +
               "   m.name LIKE CONCAT('%', :name, '%') " +
               "   OR EXISTS (" +
               "       SELECT 1 FROM AlternativeTitleModel a " +
               "       WHERE a.media = m " +
               "       AND a.name LIKE CONCAT('%', :alternativeTitle, '%')" +
               "   )" +
               ")")
    Page<MediaModel> findMediaByNameOrAlternativeTitleAndMediaCategoryIn(
            @Param("name") String name,
            @Param("alternativeTitle") String alternativeTitle,
            @Param("categories") java.util.Collection<Integer> categories,
            @PageableDefault(size = 10, page = 0) Pageable pageable);

    /**
     * Find Media by ExternalReference and TypeReference.
     *
     * @param externalReference
     * @param typeReference
     * @return a Optional of MediaModel.
     */
    @Query("SELECT m FROM MediaModel m JOIN ExternalReferenceModel e ON e MEMBER OF m.externalReference WHERE e.reference = :reference AND e.typeReference = :typeReference")
    public Optional<MediaModel> findOneMediaByExternalReferenceAndTypeReference(@Param("reference") String reference,
            @Param("typeReference") TypeReferenceModel typeReference);

    /**
     * Batch-loads the genre association for a collection of MediaModel entities.
     * Used by @BatchMapping to resolve the N+1 problem for genres.
     *
     * @param medias the collection of MediaModel entities to load genres for.
     * @return a list of MediaModel entities with their genre collections initialized.
     */
    @Query("SELECT DISTINCT m FROM MediaModel m LEFT JOIN FETCH m.genre WHERE m IN :medias")
    List<MediaModel> findAllWithGenreByMediaIn(@Param("medias") Collection<MediaModel> medias);

    /**
     * Batch-loads the company association for a collection of MediaModel entities.
     * Used by @BatchMapping to resolve the N+1 problem for companies.
     *
     * @param medias the collection of MediaModel entities to load companies for.
     * @return a list of MediaModel entities with their company collections initialized.
     */
    @Query("SELECT DISTINCT m FROM MediaModel m LEFT JOIN FETCH m.company WHERE m IN :medias")
    List<MediaModel> findAllWithCompanyByMediaIn(@Param("medias") Collection<MediaModel> medias);

    /**
     * Batch-loads the people association for a collection of MediaModel entities.
     * Used by @BatchMapping to resolve the N+1 problem for people.
     *
     * @param medias the collection of MediaModel entities to load people for.
     * @return a list of MediaModel entities with their people collections initialized.
     */
    @Query("SELECT DISTINCT m FROM MediaModel m LEFT JOIN FETCH m.people WHERE m IN :medias")
    List<MediaModel> findAllWithPeopleByMediaIn(@Param("medias") Collection<MediaModel> medias);
}
