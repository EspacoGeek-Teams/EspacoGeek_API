package com.espacogeek.geek.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.espacogeek.geek.models.UserMediaListModel;

public interface UserMediaListRepository extends JpaRepository<UserMediaListModel, UUID> {

    /**
     * Fetches library entries for a given user with optional filters on:
     * status string, status ID (media's production status), media category ID,
     * category name, genre ID, genre name, media ID, media name, and alternative title.
     *
     * @param userId       the ID of the authenticated user (required)
     * @param status       optional user tracking status string filter (case-insensitive exact match)
     * @param statusId     optional media production status ID filter (MediaStatusModel.id, Integer)
     * @param categoryId   optional media category ID filter
     * @param categoryName optional media category name filter (case-insensitive exact match)
     * @param genreId      optional genre ID filter
     * @param genreName    optional genre name filter (case-insensitive exact match)
     * @param mediaId      optional media ID filter
     * @param mediaName    optional media name filter (case-insensitive partial match)
     * @param altTitle     optional alternative title filter (case-insensitive partial match)
     * @return a distinct list of matching library entries
     */
    @Query("SELECT DISTINCT u FROM UserMediaListModel u " +
           "JOIN u.media m " +
           "WHERE u.user.id = :userId " +
           "AND (:status IS NULL OR LOWER(u.status) = LOWER(:status)) " +
           "AND (:statusId IS NULL OR (m.mediaStatus IS NOT NULL AND m.mediaStatus.id = :statusId)) " +
           "AND (:categoryId IS NULL OR m.mediaCategory.id = :categoryId) " +
           "AND (:categoryName IS NULL OR LOWER(CAST(m.mediaCategory.name AS string)) = LOWER(:categoryName)) " +
           "AND (:genreId IS NULL OR EXISTS (SELECT 1 FROM GenreModel g WHERE g MEMBER OF m.genre AND g.id = :genreId)) " +
           "AND (:genreName IS NULL OR EXISTS (SELECT 1 FROM GenreModel g WHERE g MEMBER OF m.genre AND LOWER(g.name) = LOWER(:genreName))) " +
           "AND (:mediaId IS NULL OR m.id = :mediaId) " +
           "AND (:mediaName IS NULL OR LOWER(m.name) LIKE LOWER(CONCAT('%', :mediaName, '%'))) " +
           "AND (:altTitle IS NULL OR EXISTS (SELECT 1 FROM AlternativeTitleModel a WHERE a.media = m AND LOWER(a.name) LIKE LOWER(CONCAT('%', :altTitle, '%'))))")
    List<UserMediaListModel> findByUserIdWithFilters(
            @Param("userId") Integer userId,
            @Param("status") String status,
            @Param("statusId") Integer statusId,
            @Param("categoryId") Integer categoryId,
            @Param("categoryName") String categoryName,
            @Param("genreId") Integer genreId,
            @Param("genreName") String genreName,
            @Param("mediaId") Integer mediaId,
            @Param("mediaName") String mediaName,
            @Param("altTitle") String altTitle);
}
