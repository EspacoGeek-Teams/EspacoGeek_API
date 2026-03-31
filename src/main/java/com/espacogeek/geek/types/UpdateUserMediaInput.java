package com.espacogeek.geek.types;

import java.util.Date;

import com.espacogeek.geek.models.StatusType;

import lombok.Getter;
import lombok.Setter;

/**
 * GraphQL input type for creating or updating a user's media list entry.
 * Used by the {@code upsertUserMedia} mutation, which is capable of both
 * adding a new entry and updating an existing one identified by the authenticated
 * user's ID and the provided {@code mediaId}.
 */
@Getter
@Setter
public class UpdateUserMediaInput {

    /** ID of the media item to add or update in the user's library. Required for upsert. */
    private Integer mediaId;

    /** Tracking status (e.g. PLANNING, IN_PROGRESS, COMPLETED, DROPPED, PAUSED). */
    private StatusType status;

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

    /** ID of the user's custom status list to assign this media to (optional). */
    private Integer customStatusId;

    /** Number of times rewatched/replayed (default 0 for new entries). */
    private Integer rewatchCount;

    /** Whether this library entry is private (default false for new entries). */
    private Boolean isPrivate;

    /** Personal notes separate from the main review note. */
    private String personalNotes;
}
