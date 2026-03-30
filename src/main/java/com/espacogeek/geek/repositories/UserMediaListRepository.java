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
     * status, media category, genre name, media name, and alternative title.
     *
     * @param userId     the ID of the authenticated user (required)
     * @param status     optional status filter (case-insensitive exact match)
     * @param categoryId optional media category ID filter
     * @param genreName  optional genre name filter (case-insensitive exact match)
     * @param mediaName  optional media name filter (case-insensitive partial match)
     * @param altTitle   optional alternative title filter (case-insensitive partial match)
     * @return a distinct list of matching library entries
     */
    @Query("SELECT DISTINCT u FROM UserMediaListModel u " +
           "JOIN u.media m " +
           "WHERE u.user.id = :userId " +
           "AND (:status IS NULL OR LOWER(u.status) = LOWER(:status)) " +
           "AND (:categoryId IS NULL OR m.mediaCategory.id = :categoryId) " +
           "AND (:genreName IS NULL OR EXISTS (SELECT 1 FROM GenreModel g WHERE g MEMBER OF m.genre AND LOWER(g.name) = LOWER(:genreName))) " +
           "AND (:mediaName IS NULL OR LOWER(m.name) LIKE LOWER(CONCAT('%', :mediaName, '%'))) " +
           "AND (:altTitle IS NULL OR EXISTS (SELECT 1 FROM AlternativeTitleModel a WHERE a.media = m AND LOWER(a.name) LIKE LOWER(CONCAT('%', :altTitle, '%'))))")
    List<UserMediaListModel> findByUserIdWithFilters(
            @Param("userId") Integer userId,
            @Param("status") String status,
            @Param("categoryId") Integer categoryId,
            @Param("genreName") String genreName,
            @Param("mediaName") String mediaName,
            @Param("altTitle") String altTitle);
}
