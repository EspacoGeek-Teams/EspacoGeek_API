package com.espacogeek.geek.services;

import java.util.List;

import com.espacogeek.geek.models.MediaStatusModel;

/**
 * Interface for the MediaStatusService, which provides methods for managing MediaStatusModel objects.
 */
public interface MediaStatusService {
    /**
     * Retrieves all MediaStatusModel objects.
     *
     * @return A list of all MediaStatusModel objects.
     */
    List<MediaStatusModel> findAll();
}
