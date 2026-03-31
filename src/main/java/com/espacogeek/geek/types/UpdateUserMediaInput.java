package com.espacogeek.geek.types;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

/**
 * GraphQL input type for creating or updating a user's media list entry.
 * Used by the {@code userMediaProgress} mutation, which is capable of both
 * adding a new entry and updating an existing one identified by the authenticated
 * user's ID and the provided {@code mediaId}.
 */
@Getter
@Setter
public class UpdateUserMediaInput {

    /** ID of the media item to add or update in the user's library. Required for upsert. */
    private Integer mediaId;

    /** Tracking status (e.g. PLANNING, IN_PROGRESS, COMPLETED, DROPPED, PAUSED). */
    private String status;

    /** Current progress (e.g. episode number). */
    private Integer progress;

    /** User score for this media (0.0 – 10.0). */
    private Double score;

    /** Date the user started the media. */
    private Date startDate;

    /** Date the user finished the media. */
    private Date finishDate;

    /** User note or review. */
    private String note;
}
